package flashtanki.server.battles.mode

import flashtanki.server.ServerMapDominationPoint
import flashtanki.server.battles.Battle
import flashtanki.server.battles.BattleMode
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import flashtanki.server.battles.BattlePlayer
import flashtanki.server.battles.BattleTank
import flashtanki.server.client.*
import flashtanki.server.battles.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.*
import flashtanki.server.quests.*
import mu.KotlinLogging
import flashtanki.server.battles.sendTo
import flashtanki.server.battles.BattleTeam
import flashtanki.server.commands.Command
import flashtanki.server.commands.CommandName
import flashtanki.server.commands.CommandHandler
import flashtanki.server.commands.ICommandHandler

data class PointState(
    val info: ServerMapDominationPoint,
    val occupated_users: MutableList<String> = mutableListOf<String>(),
    var progress: Int = 0,
    var state: String = "neutral"
)

fun PointState.toDomPoint(): DomPoint {
    return DomPoint(
        id = info.id,
        radius = info.distance,
        x = info.position.x,
        y = info.position.y,
        z = info.position.z,
        score = progress,
        state = state,
        occupated_users = occupated_users
    )
}

class ControlPointsModeHandler(battle: Battle) : ICommandHandler, TeamModeHandler(battle) {
    companion object {
        fun builder(): BattleModeHandlerBuilder = { battle -> ControlPointsModeHandler(battle) }
    }

    override val mode: BattleMode get() = BattleMode.ControlPoints

    var points = mutableListOf<PointState>()
    private val logger = KotlinLogging.logger { }
    var pointJob: Job = GlobalScope.launch {}
    var pointJobs = mutableListOf<Job>()
    var pointControlJobs = mutableMapOf<Int, Job>() // New map to manage control jobs

    /*init {
        val mapPoints = battle.map.points ?: throw IllegalStateException("Map has no domination points")
        points += mapPoints.map { point ->
            PointState(point, state = battle.properties[BattleProperty.ScoreLimit.tank] ?: "neutral")
        }
    } //надо фиксануть этот закоменченный инит
     */

    init {
        val mapPoints = battle.map.points ?: throw IllegalStateException("Map has no domination points")
        points += mapPoints.map { point -> PointState(point) }
    }

    fun getPointId(point: Int): String {
        return when (point) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            3 -> "D"
            4 -> "E"
            5 -> "F"
            6 -> "G"
            else -> "A"
        }
    }

    suspend fun tankCapturingPoint(tank: BattleTank, pointId: Int) {
        val name = tank.id.toString()
        val point = points[pointId]
        if (point.occupated_users.isEmpty()) {
            Command(
                CommandName.TankCapturingPoint,
                getPointId(pointId),
                name
            ).sendTo(tank.battle)
            pointJob = tank.battle.coroutineScope.launch {
                if (tank.player.team == BattleTeam.Red) {
                    while (point.progress >= -100) {
                        delay(25)
                        point.progress--
                        if (point.progress == 0) {
                            lostBy(getPointId(pointId), getTeamByTank(tank), tank)
                            point.state = "neutral"
                        }
                        Command(
                            CommandName.SetPoinScore,
                            getPointId(pointId),
                            point.progress.toString(),
                            0.toString()
                        ).sendTo(tank.battle)
                        if (point.progress == -100) {
                            capturedBy(getPointId(pointId), getTeamByTank(tank), tank)
                            point.state = getTeamByTank(tank)
                            handleCapture(point, tank)
                        }
                    }
                } else if (tank.player.team == BattleTeam.Blue) {
                    while (point.progress <= 100) {
                        delay(25)
                        point.progress++
                        if (point.progress == 0) {
                            lostBy(getPointId(pointId), getTeamByTank(tank), tank)
                            point.state = "neutral"
                        }
                        Command(
                            CommandName.SetPoinScore,
                            getPointId(pointId),
                            point.progress.toString(),
                            0.toString()
                        ).sendTo(tank.battle)
                        if (point.progress == 100) {
                            capturedBy(getPointId(pointId), getTeamByTank(tank), tank)
                            point.state = getTeamByTank(tank)
                            handleCapture(point, tank)
                        }
                    }
                }
            }
            pointJobs.add(pointJob)
            point.occupated_users.add(name)
        }
    }

    suspend fun handleCapture(point: PointState, tank: BattleTank) {
        val enemyPlayerCount = tank.battle.players.size
        if (enemyPlayerCount > 1 && !battle.properties[BattleProperty.ParkourMode]) {
            val fundProcessor = tank.battle.fundProcessor
            fundProcessor.fund += 8
            fundProcessor.updateFund()
        }
        tank.player.user.questOf<CapturePointQuest>()?.let { quest ->
            quest.current++
            tank.socket.updateQuests()
            quest.updateProgress()
        }
        addScoreToTeam(point, tank)
    }

    suspend fun addScoreToTeam(point: PointState, tank: BattleTank) {
        val scoreLimit = tank.battle.properties[BattleProperty.ScoreLimit]
        val pointId = points.indexOf(point)

        pointControlJobs[pointId]?.cancel()

        val job = tank.battle.coroutineScope.launch {
            while (point.progress <= -100 || point.progress >= 100) {
                delay(2000)
                teamScores.merge(tank.player.team, 1, Int::plus)
                updateScores()
                if (scoreLimit != 0 && teamScores[tank.player.team] == scoreLimit && battle.modeHandler is ControlPointsModeHandler) {
                    tank.battle.restart()
                    resetPoints()
                    break
                }
            }
        }
        pointControlJobs[pointId] = job
    }

    fun resetPoints() {
        for (point in points) {
            point.occupated_users.clear()
            point.state = "neutral"
            point.progress = 0
        }
        pointControlJobs.values.forEach { it.cancel() }
        pointControlJobs.clear()
    }

    suspend fun tankLeaveCapturingPoint(tank: BattleTank, pointId: Int) {
        val name = tank.id.toString()
        val point = points[pointId]
        Command(
            CommandName.TankLeaveCapturingPoint,
            name,
            getPointId(pointId)
        ).sendTo(tank.battle)
        pointJobs[point.occupated_users.indexOf(tank.id.toString())].cancel()
        pointJobs.removeAt(point.occupated_users.indexOf(tank.id.toString()))
        point.occupated_users.remove(name)
        if (point.occupated_users.isEmpty() && point.progress != 0 && (point.progress > -100 && point.progress < 100)) {
            returnToNeutral(point, tank)
        }
    }
    suspend fun returnToNeutral(point: PointState, tank: BattleTank) {
        val team = getTeamByPointState(point)
        while (point.progress != 0) {
            delay(200)
            if (team == BattleTeam.Red) point.progress++ else point.progress--
            Command(
                CommandName.SetPoinScore,
                getPointId(points.indexOf(point)),
                point.progress.toString(),
                0.toString()
            ).sendTo(tank.battle)
        }
    }

    fun getTeamByPointState(point: PointState): BattleTeam {
        return when (point.state) {
            "red" -> BattleTeam.Red
            "blue" -> BattleTeam.Blue
            else -> BattleTeam.None
        }
    }

    fun getTeamByTank(tank: BattleTank): String {
        return when (tank.player.team) {
            BattleTeam.Red -> "red"
            BattleTeam.Blue -> "blue"
            else -> "neutral"
        }
    }

    suspend fun capturedBy(id: String, by: String, tank: BattleTank) {
        Command(
            CommandName.PointCapturedBy,
            by,
            id
        ).sendTo(tank.battle)
    }

    suspend fun lostBy(id: String, by: String, tank: BattleTank) {
        Command(
            CommandName.PointLostBy,
            by,
            id
        ).sendTo(tank.battle)
    }

    override suspend fun initModeModel(player: BattlePlayer) {
        Command(
            CommandName.InitDomModel,
            InitDomModelData(
                resources = DomModelResources().toJson(),
                lighting = DomModelLighting().toJson(),
                points = points.map { point -> point.toDomPoint() },
                mine_activation_radius = 5
            ).toJson()
        ).send(player)
    }
}
