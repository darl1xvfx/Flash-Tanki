package flashtanki.server.discord.commands

import flashtanki.server.ISocketServer
import flashtanki.server.bot.discord.ENlocale
import flashtanki.server.bot.discord.RUlocale
import flashtanki.server.client.IUserRepository
import flashtanki.server.client.Screen
import flashtanki.server.invite.IInviteRepository
import flashtanki.server.invite.IInviteService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.random.Random
import kotlin.system.exitProcess

class CommandHandler : KoinComponent {
    private val socketServer by inject<ISocketServer>()
    private val inviteService by inject<IInviteService>()
    private val inviteRepository by inject<IInviteRepository>()
    private val userRepository by inject<IUserRepository>()

    private val ruPrefix = "ru?"
    private val enPrefix = "en?"

    private val logger: Logger = LoggerFactory.getLogger(CommandHandler::class.java)

    private fun generateRandomCode(length: Int): String {
        val characters =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[{]};:'\",<.>/?ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[{]};:'\",<.>/?"
        return (1..length)
            .map { Random.nextInt(0, characters.length) }
            .map(characters::get)
            .joinToString("")
    }

    private fun buildHelpMessage(locale: String): String {
        return when (locale) {
            "ru" -> RUlocale().getHelpMessage()
            "en" -> ENlocale().getHelpMessage()
            else -> ""
        }
    }

    suspend fun handleCommand(event: GuildMessageReceivedEvent) {
        val message = event.message.contentRaw
        val channel = event.channel
        val userId = event.author.id
        val DiscordUserID = setOf("531060542740234240", "1185301918290022532", "1140945846146445354","994657015257378997","531366173875372032","929665438437240923","1139886895715848222")

        if (userId !in DiscordUserID) {
            logger.info("Allowed: $DiscordUserID, UserId: $userId")
            return
        }

        val prefix = when {
            message.startsWith(ruPrefix) -> ruPrefix
            message.startsWith(enPrefix) -> enPrefix
            else -> return
        }

        val locale = if (prefix == ruPrefix) "ru" else "en"

        when {
            message.startsWith(prefix + "stop") -> {
                GlobalScope.launch {
                    logger.info("\u001B[31mRequest to shutdown server received stop...\u001B[0m")
                    channel.sendMessage(
                        if (locale == "ru") RUlocale().serverStopped(event.member?.asMention)
                        else ENlocale().serverStopped(event.member?.asMention)
                    ).queue()
                    delay(5000)
                    exitProcess(0)
                }
            }

            message.startsWith(prefix + "online") -> GlobalScope.launch {
                val playersByScreen = socketServer.players.groupBy { it.screen }
                val onlinePlayersMessage = buildString {
                    append("__**Online**__: ${socketServer.players.size}\n")
                    append(
                        "__**Players**__: ${
                            socketServer.players.mapNotNull { it.user?.username }.joinToString(", ")
                                .takeIf { it.isNotBlank() } ?: "None"
                        }\n")

                    fun buildScreenMessage(screen: Screen, screenName: String) {
                        val players = playersByScreen[screen]?.mapNotNull { it.user?.username }?.joinToString(", ")
                        val message = if (players.isNullOrBlank()) "None" else players
                        append("__**Players in $screenName**__: $message\n")
                    }

                    buildScreenMessage(Screen.Battle, if (locale == "ru") "бою" else "battle")
                    buildScreenMessage(Screen.BattleSelect, if (locale == "ru") "выборе боев" else "choosing battles")
                    buildScreenMessage(Screen.Garage, if (locale == "ru") "гараже" else "the garage")
                }

                channel.sendMessage(onlinePlayersMessage.trim()).queue()
            }

            message.startsWith(prefix + "invite") -> {
                val args = message.removePrefix(prefix + "invite").trim().split("\\s+".toRegex())
                val subcommand = args.getOrElse(0) { "" }

                when (subcommand) {
                    "toggle" -> {
                        inviteService.enabled = !inviteService.enabled
                        channel.sendMessage(
                            if (locale == "ru") RUlocale().toggleInvite(inviteService.enabled)
                            else ENlocale().toggleInvite(inviteService.enabled)
                        ).queue()
                        logger.info(if (inviteService.enabled) "\u001B[32mInvite codes are now: enabled\u001B[0m" else "\u001B[31mInvite codes are now: not enabled\u001B[0m")
                    }

                    "add" -> {
                        val code = args.getOrElse(1) { "" }
                        inviteRepository.createInvite(code)
                        channel.sendMessage(
                            if (locale == "ru") RUlocale().inviteAdded(code)
                            else ENlocale().inviteAdded(code)
                        ).queue()
                    }

                    "delete" -> {
                        val code = args.getOrElse(1) { "" }
                        val deleted = inviteRepository.deleteInvite(code)
                        channel.sendMessage(
                            if (locale == "ru") RUlocale().inviteDeleted(deleted, code)
                            else ENlocale().inviteDeleted(deleted, code)
                        ).queue()
                    }

                    "list" -> {
                        val invites = inviteRepository.getInvites()
                        if (invites.isEmpty()) {
                            val message = if (locale == "ru") {
                                RUlocale().noInviteCodes()
                            } else {
                                ENlocale().noInviteCodes()
                            }
                            channel.sendMessage(message).queue()
                            return
                        }

                        val inviteList = invites.joinToString("\n") { invite ->
                            val code = invite.code
                            val username = invite.username ?: if (locale == "ru") {
                                RUlocale().unknownUsername()
                            } else {
                                ENlocale().unknownUsername()
                            }
                            val id = invite.id.toString()
                            val localizedMessage = if (locale == "ru") {
                                RUlocale().inviteListEntry(code, username, id)
                            } else {
                                ENlocale().inviteListEntry(code, username, id)
                            }
                            localizedMessage
                        }

                        channel.sendMessage(inviteList).queue()
                    }

                    "give" -> {
                        val mentionedUsers = event.message.mentionedUsers
                        if (mentionedUsers.isNotEmpty()) {
                            val generatedCode = generateRandomCode(20)
                            inviteRepository.createInvite(generatedCode)

                            mentionedUsers.forEach { user ->
                                user.openPrivateChannel().queue { privateChannel ->
                                    privateChannel.sendMessage(
                                        if (locale == "ru") RUlocale().inviteSent(generatedCode)
                                        else ENlocale().inviteSent(generatedCode)
                                    ).queue()
                                }
                                channel.sendMessage(
                                    if (locale == "ru") RUlocale().inviteSentToUser(user.name)
                                    else ENlocale().inviteSentToUser(user.name)
                                ).queue()
                            }
                        } else {
                            channel.sendMessage(
                                if (locale == "ru") RUlocale().mentionUserForInvite()
                                else ENlocale().mentionUserForInvite()
                            ).queue()
                        }
                    }

                    else -> {
                        channel.sendMessage(
                            if (locale == "ru") RUlocale().invalidCommand()
                            else ENlocale().invalidCommand()
                        ).queue()
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

                            channel.sendMessage(
                                if (locale == "ru") RUlocale().crystalsAdded(amount, user.username)
                                else ENlocale().crystalsAdded(amount, user.username)
                            ).queue()
                        } else {
                            channel.sendMessage(
                                if (locale == "ru") RUlocale().userNotFound(username)
                                else ENlocale().userNotFound(username)
                            ).queue()
                        }
                    } else {
                        channel.sendMessage(
                            if (locale == "ru") RUlocale().invalidCrystalsAmount()
                            else ENlocale().invalidCrystalsAmount()
                        ).queue()
                    }
                } else {
                    channel.sendMessage(
                        if (locale == "ru") RUlocale().incorrectAddCryFormat()
                        else ENlocale().incorrectAddCryFormat()
                    ).queue()
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

                            channel.sendMessage(
                                if (locale == "ru") RUlocale().scoreAdded(amount, user.username)
                                else ENlocale().scoreAdded(amount, user.username)
                            ).queue()
                        } else {
                            channel.sendMessage(
                                if (locale == "ru") RUlocale().userNotFound(username)
                                else ENlocale().userNotFound(username)
                            ).queue()
                        }
                    } else {
                        channel.sendMessage(
                            if (locale == "ru") RUlocale().invalidScoreAmount()
                            else ENlocale().invalidScoreAmount()
                        ).queue()
                    }
                } else {
                    channel.sendMessage(
                        if (locale == "ru") RUlocale().incorrectAddScoreFormat()
                        else ENlocale().incorrectAddScoreFormat()
                    ).queue()
                }
            }

            message.startsWith(prefix + "help") -> {
                channel.sendMessage(buildHelpMessage(locale)).queue()
            }

            else -> {
                channel.sendMessage(
                    if (locale == "ru") RUlocale().invalidCommand()
                    else ENlocale().invalidCommand()
                ).queue()
            }
        }
    }
}