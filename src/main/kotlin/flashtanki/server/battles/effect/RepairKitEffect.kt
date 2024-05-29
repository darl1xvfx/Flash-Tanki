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
    private var isActive: Boolean = false
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

        // Проверка, есть ли у танка полное здоровье
        if (tank.health >= maxHealth) {
            return
        }

        isActive = true
        val battle = tank.battle
        val damageProcessor = battle.damageProcessor

        val initialHealth = tank.health

        damageProcessor.heal(tank, 1000.0)
        Command(CommandName.DamageTank, tank.id, "1000", DamageType.Heal.key).send(tank)
        damageProcessor.heal(tank, 100.0)

        val initialHealingAmount = minOf(100.0, maxHealth - initialHealth)
        if (initialHealingAmount > 0) {
            damageProcessor.heal(tank, initialHealingAmount)
            Command(CommandName.DamageTank, tank.id, "100", DamageType.Heal.key).send(tank) // Визуальное представление
        }

        if (duration == null) return

        tank.coroutineScope.launch {
            val startTime = Clock.System.now()
            val endTime = startTime + duration
            while (Clock.System.now() < endTime && isActive) {
                delay(50)
                if (tank.health < maxHealth) {
                    val remainingHealAmount = minOf(100.0, maxHealth - tank.health)
                    if (remainingHealAmount > 0) {
                        damageProcessor.heal(tank, remainingHealAmount)
                        Command(CommandName.DamageTank, tank.id, "100", DamageType.Heal.key).send(tank) // Визуальное представление
                        totalHealing += remainingHealAmount
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
            val totalHealingNeeded = maxHealth - currentHealth
            val healingRatePerInterval = 100.0
            val intervalsPerSecond = 1000.0 / 30.0

            val totalIntervalsNeeded = totalHealingNeeded / healingRatePerInterval
            val durationInSeconds = totalIntervalsNeeded / intervalsPerSecond
            return durationInSeconds.seconds
        }
    }
}
