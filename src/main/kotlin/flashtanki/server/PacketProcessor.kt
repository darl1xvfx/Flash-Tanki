package flashtanki.server

import java.io.ByteArrayOutputStream
import mu.KotlinLogging
import flashtanki.server.commands.Command
import flashtanki.server.extensions.indexOfSequence

class PacketProcessor {
  private val logger = KotlinLogging.logger {}

  private val output = ByteArrayOutputStream()

  fun write(data: ByteArray) = synchronized(output) {
    output.write(data)

    // logger.trace { "Written: ${String(data)}" }
  }

  fun tryGetPacket(): String? = synchronized(output) {
    val buffer = output.toByteArray()

    val position = buffer.indexOfSequence(Command.Delimiter)
    if(position == -1) return null

    val packet = buffer.decodeToString(0, position)

    val offset = position + Command.Delimiter.size
    output.reset()
    output.write(buffer, offset, buffer.size - offset)

    // logger.trace { "End of packet: $packet" }

    return packet
  }
}
