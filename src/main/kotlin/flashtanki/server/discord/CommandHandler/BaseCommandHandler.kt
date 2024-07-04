package flashtanki.server.discord.CommandHandler

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import flashtanki.server.ISocketServer
import flashtanki.server.client.IUserRepository
import flashtanki.server.client.Screen
import flashtanki.server.invite.IInviteRepository
import flashtanki.server.invite.IInviteService
import kotlin.random.Random
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger { }

abstract class BaseCommandHandler(
    protected val prefix: String
) : KoinComponent {
    protected val socketServer by inject<ISocketServer>()
    protected val inviteService by inject<IInviteService>()
    protected val inviteRepository by inject<IInviteRepository>()
    protected val userRepository by inject<IUserRepository>()

    protected fun generateRandomCode(length: Int): String {
        val characters =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[{]};:'\",<.>/?ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[{]};:'\",<.>/?"
        return (1..length)
            .map { Random.nextInt(0, characters.length) }
            .map(characters::get)
            .joinToString("")
    }

    protected abstract fun buildHelpMessage(): String

    suspend fun handleCommand(event: GuildMessageReceivedEvent) {
        val (message, channel, userId) = Triple(event.message.contentRaw, event.channel, event.author.id)
        val DiscordUserID = setOf("531060542740234240", "1185301918290022532", "1140945846146445354","994657015257378997","531366173875372032")

        if (userId !in DiscordUserID) {
            logger.info("Allowed: $DiscordUserID, UserId: $userId")
            return
        }

        when {
            message.startsWith(prefix + "stop") -> {
                GlobalScope.launch {
                    logger.info("\u001B[31mRequest to shutdown server received stop...\u001B[0m")
                    channel.sendMessage(localizeStopMessage(event.member?.asMention ?: "Unknown")).queue()
                    delay(5000)
                    exitProcess(0)
                }
            }

            message.startsWith(prefix + "online") -> GlobalScope.launch {
                val playersByScreen = socketServer.players.groupBy { it.screen }
                val onlinePlayersMessage = buildString {
                    append(localizeOnlineMessageHeader(socketServer.players.size))
                    append(
                        localizePlayerListMessage(
                            socketServer.players.mapNotNull { it.user?.username }.joinToString(", ")
                                .takeIf { it.isNotBlank() } ?: localizeNoPlayersMessage()
                        )
                    )

                    fun buildScreenMessage(screen: Screen, screenName: String) {
                        val players = playersByScreen[screen]?.mapNotNull { it.user?.username }?.joinToString(", ")
                        val message = if (players.isNullOrBlank()) localizeNoPlayersMessage() else players
                        append(localizeScreenMessage(screenName, message))
                    }

                    buildScreenMessage(Screen.Battle, "battle")
                    buildScreenMessage(Screen.BattleSelect, "choosing battles")
                    buildScreenMessage(Screen.Garage, "the garage")
                }

                channel.sendMessage(onlinePlayersMessage.trim()).queue()
            }

            message.startsWith(prefix + "invite") -> {
                val args = message.removePrefix(prefix + "invite").trim().split("\\s+".toRegex())
                val subcommand = args.getOrElse(0) { "" }

                when (subcommand) {
                    "toggle" -> {
                        inviteService.enabled = !inviteService.enabled
                        channel.sendMessage(localizeInviteToggleMessage(inviteService.enabled)).queue()
                        logger.info(if (inviteService.enabled) "\u001B[32mInvite codes are now: enabled\u001B[0m" else "\u001B[31mInvite codes are now: not enabled\u001B[0m")
                    }

                    "add" -> {
                        val code = args.getOrElse(1) { "" }
                        inviteRepository.createInvite(code)
                        channel.sendMessage(localizeInviteAddMessage(code)).queue()
                    }

                    "delete" -> {
                        val code = args.getOrElse(1) { "" }
                        val deleted = inviteRepository.deleteInvite(code)
                        channel.sendMessage(localizeInviteDeleteMessage(code, deleted)).queue()
                    }

                    "list" -> {
                        val invites = inviteRepository.getInvites()
                        if (invites.isEmpty()) {
                            channel.sendMessage(localizeNoInviteCodesMessage()).queue()
                            return
                        }
                        val inviteList = invites.joinToString("\n") { invite -> " - ${invite.code} (ID: ${invite.id})" }
                        channel.sendMessage(inviteList).queue()
                    }

                    "give" -> {
                        val mentionedUsers = event.message.mentionedUsers
                        if (mentionedUsers.isNotEmpty()) {
                            val generatedCode = generateRandomCode(20)
                            inviteRepository.createInvite(generatedCode)

                            mentionedUsers.forEach { user ->
                                user.openPrivateChannel().queue { privateChannel ->
                                    privateChannel.sendMessage(localizeInviteSendMessage(generatedCode)).queue()
                                }
                                channel.sendMessage(localizeInviteSuccessMessage(user.name)).queue()
                            }
                        } else {
                            channel.sendMessage(localizeMentionUserMessage()).queue()
                        }
                    }

                    else -> {
                        channel.sendMessage(localizeInvalidInviteCommandMessage()).queue()
                    }
                }
            }

            message.startsWith(prefix + "addcry") -> GlobalScope.launch {
                val args = message.split("\\s+".toRegex())

                if (args.size >= 3) {
                    val amount = args[1].toIntOrNull()
                    val username = args[2]

                    if (amount != null) {
                        val player = socketServer.players.find { it.user?.username == username }

                        if (player != null) {
                            val user = player.user ?: throw Exception("Пользователь недействителен")

                            user.crystals = (user.crystals + amount).coerceAtLeast(0)
                            player.updateCrystals()
                            userRepository.updateUser(user)

                            channel.sendMessage(localizeAddCrystalsMessage(amount, user.username)).queue()
                        } else {
                            channel.sendMessage(localizeUserNotFoundMessage(username)).queue()
                        }
                    } else {
                        channel.sendMessage(localizeInvalidAmountMessage()).queue()
                    }
                } else {
                    channel.sendMessage(localizeAddCrystalsFormatMessage()).queue()
                }
            }

            message.startsWith(prefix + "addscore") -> GlobalScope.launch {
                val args = message.split("\\s+".toRegex())

                if (args.size >= 3) {
                    val amount = args[1].toIntOrNull()
                    val username = args[2]

                    if (amount != null) {
                        val player = socketServer.players.find { it.user?.username == username }

                        if (player != null) {
                            val user = player.user ?: throw Exception("Пользователь недействителен")

                            user.score = (user.score + amount).coerceAtLeast(0)
                            player.updateScore()
                            userRepository.updateUser(user)

                            channel.sendMessage(localizeAddScoreMessage(amount, user.username)).queue()
                        } else {
                            channel.sendMessage(localizeUserNotFoundMessage(username)).queue()
                        }
                    } else {
                        channel.sendMessage(localizeInvalidAmountMessage()).queue()
                    }
                } else {
                    channel.sendMessage(localizeAddScoreFormatMessage()).queue()
                }
            }

            message.startsWith(prefix + "help") -> {
                channel.sendMessage(buildHelpMessage()).queue()
            }
        }
    }

    protected abstract fun localizeStopMessage(mention: String): String
    protected abstract fun localizeOnlineMessageHeader(playerCount: Int): String
    protected abstract fun localizePlayerListMessage(players: String): String
    protected abstract fun localizeNoPlayersMessage(): String
    protected abstract fun localizeScreenMessage(screenName: String, players: String): String
    protected abstract fun localizeInviteToggleMessage(enabled: Boolean): String
    protected abstract fun localizeInviteAddMessage(code: String): String
    protected abstract fun localizeInviteDeleteMessage(code: String, deleted: Boolean): String
    protected abstract fun localizeNoInviteCodesMessage(): String
    protected abstract fun localizeInviteSendMessage(code: String): String
    protected abstract fun localizeInviteSuccessMessage(username: String): String
    protected abstract fun localizeMentionUserMessage(): String
    protected abstract fun localizeInvalidInviteCommandMessage(): String
    protected abstract fun localizeAddCrystalsMessage(amount: Int, username: String): String
    protected abstract fun localizeUserNotFoundMessage(username: String): String
    protected abstract fun localizeInvalidAmountMessage(): String
    protected abstract fun localizeAddCrystalsFormatMessage(): String
    protected abstract fun localizeAddScoreMessage(amount: Int, username: String): String
    protected abstract fun localizeAddScoreFormatMessage(): String
}
