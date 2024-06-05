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
    private var isActive: Boolean = false,
) : TankEffect(
    tank,
    duration = calculateHealingDuration(tank),
    cooldown = 20.seconds // 20 sec
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
        val extraHealingDuration = 5.seconds

        isActive = true
        val battle = tank.battle
        val damageProcessor = battle.damageProcessor

        if (duration == null) return

        tank.coroutineScope.launch {
            val startTime = Clock.System.now()
            val endTime = startTime + duration

            while (isActive) {
                delay(healInterval.toLong())
                println("Tank health: ${tank.health}, Max health: $maxHealth")
                if (tank.health < maxHealth) {
                    damageProcessor.heal(tank, healPerInterval)
                    Command(CommandName.DamageTank, tank.id, healPerInterval.toString(), DamageType.Heal.key).send(tank)
                    totalHealing += healPerInterval
                } else if (!isActive) {
                    break
                } else {
                    val extraEndTime = Clock.System.now() + extraHealingDuration
                    while (Clock.System.now() < extraEndTime && isActive) {
                        delay(healInterval.toLong())
                        if (tank.health < maxHealth) {
                            damageProcessor.heal(tank, healPerInterval)
                            Command(CommandName.DamageTank, tank.id, healPerInterval.toString(), DamageType.Heal.key).send(tank)
                            totalHealing += healPerInterval
                        }
                    }
                    if (isActive) deactivated()
                    break
                }
            }
        }
    }

    private fun deactivated() {
        isActive = false
    }

    companion object {
        private fun calculateHealingDuration(tank: BattleTank): Duration {
            val maxHealth = tank.hull.modification.maxHealth
            val currentHealth = tank.health
            val totalHealingNeeded = maxHealth - currentHealth
            val healingRatePerSecond = 200.0

            val durationInSeconds = if (totalHealingNeeded > 0) {
                (totalHealingNeeded / healingRatePerSecond).seconds
            } else {
                0.seconds
            }
            return durationInSeconds
        }
    }
}
