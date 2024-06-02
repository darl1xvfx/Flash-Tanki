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
import kotlin.time.ExperimentalTime

class RepairKitEffect(
    tank: BattleTank,
    private var isActive: Boolean = false,
) : TankEffect(
    tank,
    duration = calculateHealingDuration(tank),
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

        isActive = true
        val battle = tank.battle
        val damageProcessor = battle.damageProcessor

        // Continuous healing of 300 HP per second
        if (duration == null) return

        tank.coroutineScope.launch {
            val startTime = Clock.System.now()
            val endTime = startTime + duration
            while (Clock.System.now() < endTime && isActive) {
                delay(1000)  // Heal every second
                if (tank.health < maxHealth) {
                    val remainingHealAmount = minOf(300.0, maxHealth - tank.health)
                    if (remainingHealAmount > 0) {
                        damageProcessor.heal(tank, 300.0)  // Always heal 300 HP per second
                        Command(CommandName.DamageTank, tank.id, "300", DamageType.Heal.key).send(tank)
                        totalHealing += 300.0
                    } else {
                        deactivated()
                        break
                    }
                } else {
                    deactivated()
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
            val totalHealingNeeded = maxHealth - currentHealth  // No initial healing, so no subtraction
            val healingRatePerSecond = 300.0

            val durationInSeconds = if (totalHealingNeeded > 0) {
                (totalHealingNeeded / healingRatePerSecond).seconds
            } else {
                0.seconds
            }
            return durationInSeconds
        }
    }
}
