package flashtanki.server.battles.weapons

import flashtanki.server.battles.*
import flashtanki.server.client.send
import flashtanki.server.client.weapons.flamethrower.StartFire
import flashtanki.server.client.weapons.flamethrower.StopFire
import flashtanki.server.commands.Command
import flashtanki.server.commands.CommandName
import flashtanki.server.garage.ServerGarageUserItemWeapon
import kotlinx.coroutines.delay
import kotlin.random.Random

class FlamethrowerWeaponHandler(
  player: BattlePlayer,
  weapon: ServerGarageUserItemWeapon
) : WeaponHandler(player, weapon) {
  private val random = Random.Default
  private var fire = "0"
  private var accumulatedDamage = mutableMapOf<String, Double>()

  suspend fun fireStart(startFire: StartFire) {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle


    Command(CommandName.ClientStartFire, tank.id).send(battle.players.exclude(player).ready())
  }

  suspend fun fireTarget(target: flashtanki.server.client.weapons.flamethrower.FireTarget) {
    val sourceTank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle
    fire = "0.6"

    val targetTanks = battle.players
      .mapNotNull { player -> player.tank }
      .filter { tank -> target.targets.contains(tank.id) }
      .filter { tank -> tank.state == TankState.Active }

    targetTanks.forEach { targetTank ->
      val randomDamage = random.nextInt(34, 76).toDouble()
      val damage = damageCalculator.calculate(sourceTank, targetTank)

      accumulatedDamage[targetTank.id] = (accumulatedDamage[targetTank.id] ?: 0.0) + randomDamage

      val param1 = fire
      Command(CommandName.Temperature, targetTank.id, param1).sendTo(battle)

      battle.damageProcessor.dealDamage(sourceTank, targetTank, damage.damage, damage.isCritical)

      delay(2000)
    }
  }

  suspend fun fireStop(stopFire: StopFire) {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle
    val targetTanks = battle.players.mapNotNull { it.tank }.filter { it.state == TankState.Active }

    fire = "0"

    targetTanks.forEach { targetTank ->
      Command(CommandName.ClientStopFire, tank.id).send(battle.players.exclude(player).ready())

      if (targetTank.health > 1.0) {
        val damage = damageCalculator.calculate(tank, targetTank)
        while ((accumulatedDamage[targetTank.id] ?: 0.0) > 0 && targetTank.health > 1.0) {
          val damageToDeal = damage.damage / 2.5
          battle.damageProcessor.dealDamage(player.tank!!, targetTank, damageToDeal, isCritical = false)
          accumulatedDamage[targetTank.id] = (accumulatedDamage[targetTank.id] ?: 0.0) - damageToDeal
          val currentTemperature = ((accumulatedDamage[targetTank.id] ?: 0.0)).coerceIn(0.0000, 0.5)
          Command(CommandName.Temperature, targetTank.id, currentTemperature.toString()).sendTo(battle)

          delay(2000)
        }
      } else {
        Command(CommandName.Temperature, targetTank.id, "0.0").sendTo(battle)
      }
      Command(CommandName.Temperature, targetTank.id, fire).sendTo(battle)
    }
  }
}