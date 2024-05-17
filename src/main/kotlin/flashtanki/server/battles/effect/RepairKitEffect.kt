package flashtanki.server.battles.effect

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import flashtanki.server.battles.BattleTank
import flashtanki.server.battles.DamageType
import flashtanki.server.client.send
import flashtanki.server.commands.Command
import flashtanki.server.commands.CommandName
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class RepairKitEffect(
    tank: BattleTank,
    private val isActive: Boolean = false
) : TankEffect(
    tank,
    duration = 3.seconds,
    cooldown = 20.seconds
) {
    private var totalHealing = 0.0

    override val info: EffectInfo
        get() = EffectInfo(
            id = 1,
            name = "health"
        )

    override suspend fun activate() {
        val battle = tank.battle
        val damageProcessor = battle.damageProcessor

        val initialHealth = tank.health

        val initialHealingAmount = minOf(30.0, tank.hull.modification.maxHealth - initialHealth)
        damageProcessor.heal(tank, initialHealingAmount)

        if (initialHealingAmount > 0) {
            Command(CommandName.DamageTank, tank.id, "30", DamageType.Heal.key).send(tank) // Visual representation
        }

        if (duration == null) return

        tank.coroutineScope.launch {
            val startTime = Clock.System.now()
            val endTime = startTime + duration
            while (Clock.System.now() < endTime) {
                delay(30)
                if (tank.health < tank.hull.modification.maxHealth) {
                    val remainingHealAmount = minOf(300.0, tank.hull.modification.maxHealth - tank.health)
                    if (remainingHealAmount > 0) {
                        damageProcessor.heal(tank, remainingHealAmount)
                        Command(CommandName.DamageTank, tank.id, "30", DamageType.Heal.key).send(tank) // Visual representation
                        totalHealing += remainingHealAmount
                    }
                }
            }
        }
    }
}