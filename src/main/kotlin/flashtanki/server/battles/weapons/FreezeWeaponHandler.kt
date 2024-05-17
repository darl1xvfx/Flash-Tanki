package flashtanki.server.battles.weapons

import flashtanki.server.battles.*
import flashtanki.server.client.ChangeTankSpecificationData
import flashtanki.server.client.send
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
    private var originalSpeed: Double? = null

    suspend fun fireStart(startFire: StartFire) {
        if (fireStarted) return

        val tank = player.tank ?: throw Exception("No Tank")
        val battle = player.battle
        val specification = ChangeTankSpecificationData.fromPhysics(tank.hull.modification.physics, tank.weapon.item.modification.physics)

        fireStarted = true
        
        originalSpeed = specification.speed
        
        specification.speed = -0.5
        
        sendSpecificationChange(specification)
        
        temperature = -0.5

        Command(CommandName.ClientStartFire, tank.id).send(battle.players.exclude(player).ready())
		//Command(CommandName.Temperature, tank.id, temperature.toString()).sendTo(battle)
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
                val param1 = temperature

                val isAlly = targetTank.player.team == player.team
                val friendlyFireEnabled = battle.properties[BattleProperty.FriendlyFireEnabled] ?: false

                if (!isAlly || friendlyFireEnabled) {
                    Command(CommandName.Temperature, targetTank.id, param1.toString()).sendTo(battle)
                    
                    if (temperature == -0.5) {
                        delay(3500)
                        if (temperature != -0.5) {
                            val param1Stop = 0.0
                            Command(CommandName.Temperature, targetTank.id, param1Stop.toString()).sendTo(battle)
                        }
                    }
                }
            }
        }
    }

    suspend fun fireStop(stopFire: StopFire) {
        if (!fireStarted) return

        val tank = player.tank ?: throw Exception("No Tank")
        val battle = player.battle
        val specification = ChangeTankSpecificationData.fromPhysics(tank.hull.modification.physics, tank.weapon.item.modification.physics)

        originalSpeed?.let { specification.speed = it }
        originalSpeed = null

        sendSpecificationChange(specification)
        
        fireStarted = false
        temperature = 0.0

        Command(CommandName.Temperature, tank.id, temperature.toString()).sendTo(battle)

        Command(CommandName.ClientStopFire, tank.id).send(battle.players.exclude(player).ready())
        
        delay(3500)
        if (temperature != 0.0) { //-0.5
            val param1Stop = 0.0
            Command(CommandName.Temperature, tank.id, param1Stop.toString()).sendTo(battle)
        }
    }

	private suspend fun sendSpecificationChange(specification: ChangeTankSpecificationData) {
    val tank = player.tank ?: return
    val specification = ChangeTankSpecificationData.fromPhysics(tank.hull.modification.physics, tank.weapon.item.modification.physics)
    Command(CommandName.ChangeTankSpecification, tank.id, specification.toString()).send(tank)
    }
}
