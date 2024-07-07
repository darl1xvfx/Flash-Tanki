package flashtanki.server.bot.discord

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import flashtanki.server.ISocketServer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DiscordBot(
    private val discordCommandHandler: CommandHandler,
    private val autoResponsesHandler: autoResponsesHandlers
) : ListenerAdapter(), KoinComponent {

    private val socketServer by inject<ISocketServer>()

    companion object {
        private val logger = KotlinLogging.logger {}

        fun run(token: String, discordCommandHandler: CommandHandler, autoResponsesHandler: autoResponsesHandlers) {
            try {
                val bot = DiscordBot(discordCommandHandler, autoResponsesHandler)
                val jda = JDABuilder.createDefault(token)
                    .addEventListeners(bot)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.ACTIVITY)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGE_TYPING)
                    .build()
                    .awaitReady()

                val scheduler = Executors.newScheduledThreadPool(1)
                scheduler.scheduleAtFixedRate({
                    jda.presence.activity = Activity.streaming(
                        "❗Flash Tanki Online: ${bot.socketServer.players.size}",
                        ""
                    )
                }, 1, 1, TimeUnit.SECONDS)

                logger.info { "\u001B[93mBot is running!\u001B[0m" }
                logger.info { "\u001B[93mCommandHandler is running!\u001B[0m" }
            } catch (e: Exception) {
                logger.error("Failed to start bot: {}", e.message)
            }
        }
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            discordCommandHandler.handleCommand(event)
            autoResponsesHandler.handleCommand(event)
        }
    }
}
