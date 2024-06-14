package flashtanki.server.battles.bonus

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import flashtanki.server.ServerMapBonusPoint
import kotlinx.coroutines.delay
import flashtanki.server.extensions.launchDelayed
import flashtanki.server.*
import flashtanki.server.battles.Battle
import flashtanki.server.battles.BattleTank
import flashtanki.server.battles.sendTo
import flashtanki.server.commands.Command
import flashtanki.server.garage.*
import flashtanki.server.commands.CommandName
import flashtanki.server.math.Quaternion
import flashtanki.server.math.Vector3

class BattleContainerBonus(battle: Battle, id: Int, position: Vector3, rotation: Quaternion, bonusPoint: ServerMapBonusPoint, siren:String="Скоро будет сброшен золотой ящик") :
  BattleBonus(battle, id, position, rotation) {
  override val type: BonusType = BonusType.Container
  val goldBoxSiren: String = siren
  val bonusPoint = bonusPoint

  override suspend fun spawn() {
    Command(CommandName.SpawnGold, goldBoxSiren, 490113.toString()).sendTo(battle)
	Command(CommandName.AddOneGoldRegion, position.x.toString(), position.y.toString(), position.z.toString(), id.toString()).sendTo(battle)
    battle.droppedGoldIds.add(id.toString())
    val spawnBonusJob = battle.coroutineScope.launchDelayed(30.seconds) {
        super.spawn()
	}
    battle.goldBoxesIntervals.add(spawnBonusJob)
  }

  override suspend fun activate(tank: BattleTank) {
    val garageItem = tank.player.user.items.singleOrNull { userItem ->
      userItem is ServerGarageUserItemLootbox && userItem.marketItem.id == "lootbox"
    } as? ServerGarageUserItemLootbox

    garageItem?.let {
      if(it.count > 0) {
        it.count += 1

        val entityManager = HibernateUtils.createEntityManager()
        try {
          entityManager.transaction.begin()
          entityManager.merge(it)
          entityManager.transaction.commit()
        } catch(error: Exception) {
          entityManager.transaction.rollback()
          throw Exception("Error while updating garage item count", error)
        } finally {
          entityManager.close()
        }
      }
    }
    Command(CommandName.RemoveOneGoldRegion, id.toString()).sendTo(battle)
    Command(CommandName.TakeGold, tank.id, false.toString()).sendTo(battle)
    battle.droppedGoldIds.remove(id.toString())
	battle.droppedGoldBoxes.remove(bonusPoint)
    if (battle.unusedGoldBoxes.isNotEmpty()) {
      battle.spawnGoldBonus()
      battle.unusedGoldBoxes.removeAt(0)
    }
  }
}
