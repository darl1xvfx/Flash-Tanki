package flashtanki.server.bot.discord

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.koin.core.component.KoinComponent


class autoResponsesHandlers : KoinComponent {

    suspend fun handleCommand(event: GuildMessageReceivedEvent) {
        val (message, channel) = Triple(event.message.contentRaw, event.channel, event.author.id)

        /*when {
            message.startsWith(" ") -> {
                GlobalScope.launch {
                    channel.sendMessage(" ").queue()
                    channel.sendMessage(" ").queue()
                }
            }
            message.startsWith(" ") -> {
                GlobalScope.launch {
                    channel.sendMessage(" ").queue()
                    channel.sendMessage(" ").queue()
                }
            }
            message.startsWith(" ") -> {
                GlobalScope.launch {
                    channel.sendMessage(" ").queue()
                    channel.sendMessage(" ").queue()
                }
            }
            message.startsWith(" ") -> {
                GlobalScope.launch {
                    channel.sendMessage("").queue()
                    channel.sendMessage(" ").queue()
                }
            }
        }*/
    }
}