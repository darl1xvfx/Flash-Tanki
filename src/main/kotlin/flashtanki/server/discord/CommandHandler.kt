package flashtanki.server.bot.discord

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

class CommandHandler : KoinComponent {
    private val socketServer by inject<ISocketServer>()
    private val inviteService by inject<IInviteService>()
    private val inviteRepository by inject<IInviteRepository>()
    private val userRepository by inject<IUserRepository>()

    private val ruPrefix = "ru?"
    private val enPrefix = "en?"

    private fun generateRandomCode(length: Int): String {
        val characters =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[{]};:'\",<.>/?ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[{]};:'\",<.>/?"
        return (1..length)
            .map { Random.nextInt(0, characters.length) }
            .map(characters::get)
            .joinToString("")
    }

    private fun buildHelpMessage(locale: String): String {
        return if (locale == "ru") {
            """
                **Список команд:**

                ru?stop - Останавливает сервер.
                ru?online - Показывает количество онлайн игроков и их данные.
                ru?invite toggle - Включает или выключает требование инвайт-кода.
                ru?invite add <код> - Добавляет новый инвайт-код.
                ru?invite delete <код> - Удаляет инвайт-код.
                ru?invite list - Список всех инвайт-кодов.
                ru?invite give - Генерирует инвайт-код для упомянутого пользователя.
                ru?addcry <количество> <пользователь> - Добавляет кристаллы указанному пользователю.
                ru?addscore <количество> <пользователь> - Добавляет очки указанному пользователю.
                ru?help - Показывает это сообщение.
            """.trimIndent()
        } else {
            """
                **Command List:**

                en?stop - Stops the server.
                en?online - Displays the number of online players and their details.
                en?invite toggle - Toggles the invite code requirement on or off.
                en?invite add <code> - Adds a new invite code.
                en?invite delete <code> - Deletes an invite code.
                en?invite list - Lists all invite codes.
                en?invite give - Generates an invite code for a mentioned user.
                en?addcry <amount> <username> - Adds crystals to the specified user.
                en?addscore <amount> <username> - Adds score to the specified user.
                en?help - Displays this help message.
            """.trimIndent()
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
                    if (locale == "ru") {
                        channel.sendMessage("Ru: Сервер остановлен игроком ${event.member?.asMention}!").queue()
                    } else {
                        channel.sendMessage("En: Server stopped for ${event.member?.asMention}!").queue()
                    }
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
                        if (locale == "ru") {
                            channel.sendMessage("Ru: Инвайт коды теперь ${if (inviteService.enabled) "нужны" else "не нужны"} для входа в игру.").queue()
                        } else {
                            channel.sendMessage("En: Invite codes are now ${if (inviteService.enabled) "enabled" else "not enabled"} to enter the game").queue()
                        }
                        logger.info(if (inviteService.enabled) "\u001B[32mInvite codes are now: enabled\u001B[0m" else "\u001B[31mInvite codes are now: not enabled\u001B[0m")
                    }

                    "add" -> {
                        val code = args.getOrElse(1) { "" }
                        inviteRepository.createInvite(code)
                        if (locale == "ru") {
                            channel.sendMessage("Ru: Инвайт код: $code. Был добавлен").queue()
                        } else {
                            channel.sendMessage("En: Invite code called: $code. Has been added").queue()
                        }
                    }

                    "delete" -> {
                        val code = args.getOrElse(1) { "" }
                        val deleted = inviteRepository.deleteInvite(code)

                        if (locale == "ru") {
                            channel.sendMessage(if (deleted) "Ru: Инвайт '$code' успешно удален." else "Ru: Инвайт '$code' не найдено").queue()
                        } else {
                            channel.sendMessage(if (deleted) "En: Successfully removed invite code '$code'" else "En: Invite '$code' not found").queue()
                        }
                    }

                    "list" -> {
                        val invites = inviteRepository.getInvites()
                        if (invites.isEmpty()) {
                            if (locale == "ru") {
                                channel.sendMessage("Ru: Нет доступных пригласительных кодов").queue()
                            } else {
                                channel.sendMessage("En: No invite codes available").queue()
                            }
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
                                    if (locale == "ru") {
                                        privateChannel.sendMessage("Ru: Твой инвайт код: $generatedCode").queue()
                                    } else {
                                        privateChannel.sendMessage("En: Your Invite Code: $generatedCode").queue()
                                    }
                                }
                                if (locale == "ru") {
                                    channel.sendMessage("Ru: Инвайт код успешно отправлен для ${user.name}.").queue()
                                } else {
                                    channel.sendMessage("En: Invite code successfully sent to ${user.name}.").queue()
                                }
                            }
                        } else {
                            if (locale == "ru") {
                                channel.sendMessage("Ru: Упомяните пользователя для отправки инвайта.").queue()
                            } else {
                                channel.sendMessage("En: Mention the user to send an invite.").queue()
                            }
                        }
                    }

                    else -> {
                        if (locale == "ru") {
                            channel.sendMessage("Ru: Неверная команда для 'invite'").queue()
                        } else {
                            channel.sendMessage("En: Invalid command for 'invite'").queue()
                        }
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

                            if (locale == "ru") {
                                channel.sendMessage("Успешно добавлено $amount кристаллов пользователю ${user.username}").queue()
                            } else {
                                channel.sendMessage("Successfully added $amount crystals to user ${user.username}").queue()
                            }
                        } else {
                            if (locale == "ru") {
                                channel.sendMessage("Пользователь не найден: $username").queue()
                            } else {
                                channel.sendMessage("User not found: $username").queue()
                            }
                        }
                    } else {
                        if (locale == "ru") {
                            channel.sendMessage("Некорректное количество кристаллов").queue()
                        } else {
                            channel.sendMessage("Incorrect amount of crystals").queue()
                        }
                    }
                } else {
                    if (locale == "ru") {
                        channel.sendMessage("Неправильный формат команды. Используйте: ru?addcry <количество> <пользователь>").queue()
                    } else {
                        channel.sendMessage("Incorrect command format. Use: en?addcry <amount> <user>").queue()
                    }
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

                            if (locale == "ru") {
                                channel.sendMessage("Успешно добавлено $amount опыта пользователю ${user.username}").queue()
                            } else {
                                channel.sendMessage("Successfully added $amount score to user ${user.username}").queue()
                            }
                        } else {
                            if (locale == "ru") {
                                channel.sendMessage("Пользователь не найден: $username").queue()
                            } else {
                                channel.sendMessage("User not found: $username").queue()
                            }
                        }
                    } else {
                        if (locale == "ru") {
                            channel.sendMessage("Некорректное количество опыта").queue()
                        } else {
                            channel.sendMessage("Incorrect amount of score").queue()
                        }
                    }
                } else {
                    if (locale == "ru") {
                        channel.sendMessage("Неправильный формат команды. Используйте: ru?addscore <количество> <пользователь>").queue()
                    } else {
                        channel.sendMessage("Incorrect command format. Use: en?addscore <amount> <user>").queue()
                    }
                }
            }

            message.startsWith(prefix + "help") -> {
                channel.sendMessage(buildHelpMessage(locale)).queue()
            }
        }
    }
}