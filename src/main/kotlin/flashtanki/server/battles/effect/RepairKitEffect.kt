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
    duration = (4000.0 / 200.0 * 0.5).seconds,
    cooldown = 20.seconds
) {
    private var totalHealing = 0.0
    private var showVisualEffect = true

    override val info: EffectInfo
        get() = EffectInfo(
            id = 1,
            name = "health"
        )

    override suspend fun activate() {
        val maxHealth = tank.hull.modification.maxHealth
        val totalHealAmount = 4000.0
        val healInterval = 500.0
        val healPerInterval = 200.0

        isActive = true
        val battle = tank.battle
        val damageProcessor = battle.damageProcessor

        if (duration == null) return

        tank.coroutineScope.launch {
            val startTime = Clock.System.now()
            val endTime = startTime + duration

            while (Clock.System.now() < endTime && isActive && totalHealing < totalHealAmount) {
                delay(healInterval.toLong())
                val remainingHealAmount = minOf(healPerInterval, totalHealAmount - totalHealing)

                if (tank.health < maxHealth) {
                    damageProcessor.heal(tank, remainingHealAmount)
                    if (!showVisualEffect) {
                        showVisualEffect = true
                    }
                    Command(CommandName.DamageTank, tank.id, remainingHealAmount.toString(), DamageType.Heal.key).send(tank)
                    totalHealing += remainingHealAmount
                } else {
                    if (showVisualEffect) {
                        showVisualEffect = false
                    }
                    damageProcessor.heal(tank, remainingHealAmount)

                    totalHealing += remainingHealAmount
                }
            }
            deactivated()
        }
    }

    private fun deactivated() {
        isActive = false
        showVisualEffect = true
    }
}
