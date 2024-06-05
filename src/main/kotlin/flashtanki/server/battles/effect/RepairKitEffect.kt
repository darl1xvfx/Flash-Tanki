package flashtanki.server.battles.effect

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import flashtanki.server.battles.BattleTank
import flashtanki.server.battles.DamageType
import flashtanki.server.client.send
import flashtanki.server.commands.Command
import flashtanki.server.commands.CommandName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RepairKitEffect(
    tank: BattleTank,
    private var isActive: Boolean = false
) : TankEffect(
    tank,
    duration = 2.seconds,
    cooldown = 20.seconds
) {
    private var totalHealing = 0.0

    override val info: EffectInfo
        get() = EffectInfo(
            id = 1,
            name = "health"
        )

    override suspend fun activate() {
        val maxHealth = tank.hull.modification.maxHealth
        val healInterval = 100.0
        val healPerInterval = 200.0

        isActive = true
        val battle = tank.battle
        val damageProcessor = battle.damageProcessor

        if (duration == null) return

        tank.coroutineScope.launch {
            val startTime = Clock.System.now()
            val endTime = startTime + duration

            while (Clock.System.now() < endTime && isActive) {
                delay(healInterval.toLong())
                if (tank.health < maxHealth) {
                    val remainingHealAmount = minOf(healPerInterval, maxHealth - tank.health)
                    damageProcessor.heal(tank, healPerInterval)
                    if (tank.health < maxHealth) {
                        Command(CommandName.DamageTank, tank.id, remainingHealAmount.toString(), DamageType.Heal.key).send(tank)
                    }
                    totalHealing += healPerInterval
                } else {
                    damageProcessor.heal(tank, healPerInterval)
                    totalHealing += healPerInterval
                }
            }
            deactivated()
        }
    }

    private fun deactivated() {
        isActive = false
    }
}
