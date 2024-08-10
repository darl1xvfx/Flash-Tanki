package flashtanki.server.battles.anticheat

import flashtanki.server.battles.mode.FlagCarryingState
import flashtanki.server.battles.mode.FlagDroppedState
import flashtanki.server.battles.mode.FlagOnPedestalState
import flashtanki.server.battles.BattleTeam
import flashtanki.server.client.UserSocket
import mu.KotlinLogging
import java.time.Instant

data class FlagCaptureRecord(
    val captureTime: Instant,
    val initialFlagState: Class<*>
)

class CaptureTheFlagAnticheatModel {
    private val logger = KotlinLogging.logger { }
    private val playerFlagRecords = mutableMapOf<UserSocket, FlagCaptureRecord>()

    fun registerFlagCapture(socket: UserSocket, initialFlagState: Class<*>) {
        val captureTime = Instant.now()
        playerFlagRecords[socket] = FlagCaptureRecord(captureTime, initialFlagState)
    }

    fun checkFlagReturn(socket: UserSocket, currentFlagState: Class<*>) {
        val record = playerFlagRecords[socket] ?: return

        val captureTime = record.captureTime
        val initialFlagState = record.initialFlagState
        val returnTime = Instant.now()

        val elapsedTime = returnTime.toEpochMilli() - captureTime.toEpochMilli()
        if (elapsedTime < 4000 && initialFlagState != currentFlagState) {
            logger.warn { "Possible cheat detected: Flag returned too quickly. Player: ${socket.battlePlayer?.user?.username}, Elapsed Time: $elapsedTime ms" }
        }

        playerFlagRecords.remove(socket)
    }
}
