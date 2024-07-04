package flashtanki.server.bot.discord

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.koin.core.component.KoinComponent

class autoResponsesHandlers : KoinComponent {

    suspend fun handleCommand(event: GuildMessageReceivedEvent) {
        val message = event.message.contentRaw.lowercase()
        val channel = event.channel
        val authorId = event.author.id

        if (message.startsWith("ссылка") || message.startsWith("link")) {
            GlobalScope.launch {
                channel.sendMessage("https://discord.gg/tjskEJ7SFb").queue()
            }
        }
        /* when {
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
        } */
    }
}
