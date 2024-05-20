package flashtanki.server.battles.weapons

import flashtanki.server.battles.*
import flashtanki.server.client.ChangeTankSpecificationData
import flashtanki.server.client.send
import flashtanki.server.client.toJson
import flashtanki.server.client.weapons.freeze.FireTarget
import flashtanki.server.client.weapons.freeze.StartFire
import flashtanki.server.client.weapons.freeze.StopFire
import flashtanki.server.commands.Command
import flashtanki.server.commands.CommandName
import flashtanki.server.garage.ServerGarageUserItemWeapon
import kotlinx.coroutines.delay

class FreezeWeaponHandler(
    player: BattlePlayer,
    weapon: ServerGarageUserItemWeapon
) : WeaponHandler(player, weapon) {
    private var fireStarted = false
    private var temperature = 0.0
    private var originalSpeed: MutableMap<String, Double?> = mutableMapOf() // Сохраняем оригинальную скорость для каждого танка
    private var speedReduced = mutableMapOf<String, Boolean>() // Отслеживаем, была ли уменьшена скорость для каждого танка
    val tank = player.tank

    suspend fun fireStart(startFire: StartFire) {
        if (fireStarted) return

        val tank = player.tank ?: throw Exception("No Tank")
        val battle = player.battle

        fireStarted = true

        Command(CommandName.ClientStartFire, tank.id).send(battle.players.exclude(player).ready())
    }

    suspend fun fireTarget(target: FireTarget) {
        val sourceTank = player.tank ?: throw Exception("No Tank")
        val battle = player.battle

        if (!fireStarted) return

        val targetTanks = battle.players
            .mapNotNull { player -> player.tank }
            .filter { tank -> target.targets.contains(tank.id) }
            .filter { tank -> tank.state == TankState.Active }

        targetTanks.forEach { targetTank ->
            val damage = damageCalculator.calculate(sourceTank, targetTank)
            if (damage.damage > 0) {
                battle.damageProcessor.dealDamage(sourceTank, targetTank, damage.damage, isCritical = damage.isCritical)

                if (speedReduced[targetTank.id] != true) {
                    val targetSpecification = ChangeTankSpecificationData.fromPhysics(targetTank.hull.modification.physics, targetTank.weapon.item.modification.physics)
                    originalSpeed[targetTank.id] = targetSpecification.speed
                    targetSpecification.speed = -0.000001
                    speedReduced[targetTank.id] = true

                    Command(CommandName.ChangeTankSpecification, targetTank.id, targetSpecification.toJson()).sendTo(battle)
                    delay(5000)
                }

                temperature = -0.5
                val param1 = temperature

                val isAlly = targetTank.player.team == player.team
                val friendlyFireEnabled = battle.properties[BattleProperty.FriendlyFireEnabled] ?: false

                if (!isAlly || friendlyFireEnabled) {
                    Command(CommandName.Temperature, targetTank.id, param1.toString()).sendTo(battle)

                    delay(3500)
                    if (temperature != -0.5) {
                        val param1Stop = 0.0
                        Command(CommandName.Temperature, targetTank.id, param1Stop.toString()).sendTo(battle)
                    }
                }
            }
        }
    }

    suspend fun fireStop(stopFire: StopFire) {
        if (!fireStarted) return

        val tank = player.tank ?: throw Exception("No Tank")
        val battle = player.battle

        fireStarted = false
        temperature = 0.0

        Command(CommandName.Temperature, tank.id, temperature.toString()).sendTo(battle)

        Command(CommandName.ClientStopFire, tank.id).send(battle.players.exclude(player).ready())

        delay(3500)
        val targetTanks = battle.players.mapNotNull { it.tank }.filter { it.state == TankState.Active }
        targetTanks.forEach { targetTank ->
            if (speedReduced[targetTank.id] == true) {
                val targetSpecification = ChangeTankSpecificationData.fromPhysics(targetTank.hull.modification.physics, targetTank.weapon.item.modification.physics)
                targetSpecification.speed = originalSpeed[targetTank.id] ?: targetSpecification.speed
                Command(CommandName.ChangeTankSpecification, targetTank.id, targetSpecification.toJson()).sendTo(battle)
                speedReduced[targetTank.id] = false
            }
        }
    }
}