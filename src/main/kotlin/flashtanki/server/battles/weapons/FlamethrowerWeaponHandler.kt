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

  suspend fun fireStart(startFire: StartFire) {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    fire = "0.5"

    Command(CommandName.ClientStartFire, tank.id).send(battle.players.exclude(player).ready())
  }

  suspend fun fireTarget(target: flashtanki.server.client.weapons.flamethrower.FireTarget) {
    val sourceTank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle
    val fires = 0.5

    if (fires > 0) {
      val targetTanks = battle.players
        .mapNotNull { player -> player.tank }
        .filter { tank -> target.targets.contains(tank.id) }
        .filter { tank -> tank.state == TankState.Active }

      targetTanks.forEach { targetTank ->
        val damage = damageCalculator.calculate(sourceTank, targetTank)
        val param1 = fire

        Command(CommandName.Temperature, targetTank.id, param1).sendTo(battle)

        battle.damageProcessor.dealDamage(sourceTank, targetTank, damage.damage, damage.isCritical)
        if (fire == "0.5") {
          delay(2000)
          if (fire != "0.5") {
            val param1Stop = "0"
            Command(CommandName.Temperature, targetTank.id, param1Stop).sendTo(battle)
          }
        }
      }
    }
  }

  suspend fun fireStop(stopFire: StopFire) {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    fire = "0"

    Command(CommandName.Temperature, tank.id, fire).sendTo(battle)

    Command(CommandName.ClientStopFire, tank.id).send(battle.players.exclude(player).ready())
  }
}