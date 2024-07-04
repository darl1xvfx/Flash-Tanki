package flashtanki.server.discord.CommandHandler

class RuCommandHandler : BaseCommandHandler(prefix = "ru?") {
    override fun buildHelpMessage(): String {
        return """
            **Список команд:**

            `ru?stop` - Останавливает сервер.
            `ru?online` - Показывает количество онлайн игроков и их детали.
            `ru?invite toggle` - Включает или выключает требование инвайт кода.
            `ru?invite add <code>` - Добавляет новый инвайт код.
            `ru?invite delete <code>` - Удаляет инвайт код.
            `ru?invite list` - Показывает все инвайт коды.
            `ru?invite give` - Генерирует инвайт код для упомянутого пользователя.
            `ru?addcry <количество> <пользователь>` - Добавляет кристаллы указанному пользователю.
            `ru?addscore <количество> <пользователь>` - Добавляет очки указанному пользователю.
            `ru?help` - Показывает это сообщение.
        """.trimIndent()
    }

    override fun localizeStopMessage(mention: String) = "`Ru:` Сервер остановлен $mention!"

    override fun localizeOnlineMessageHeader(playerCount: Int) = "__**Онлайн**__: $playerCount\n"

    override fun localizePlayerListMessage(players: String) = "__**Игроки**__: $players\n"

    override fun localizeNoPlayersMessage() = "Нет"

    override fun localizeScreenMessage(screenName: String, players: String) = "__**Игроки в $screenName**__: $players\n"

    override fun localizeInviteToggleMessage(enabled: Boolean) = if (enabled) "`Ru:` Инвайт коды теперь `нужны` для входа в игру." else "`Ru:` Инвайт коды теперь `не нужны` для входа в игру."

    override fun localizeInviteAddMessage(code: String) = "`Ru:` Инвайт код: $code. Был добавлен."

    override fun localizeInviteDeleteMessage(code: String, deleted: Boolean) = if (deleted) "`Ru:` Инвайт '$code' успешно удален." else "`Ru:` Инвайт '$code' не найдено."

    override fun localizeNoInviteCodesMessage() = "`Ru:` Нет доступных пригласительных кодов."

    override fun localizeInviteSendMessage(code: String) = "`Ru:` Твой инвайт код: `$code`"

    override fun localizeInviteSuccessMessage(username: String) = "`Ru:` Инвайт код успешно отправлен для `$username`."

    override fun localizeMentionUserMessage() = "`Ru:` Упомяните пользователя для отправки инвайта."

    override fun localizeInvalidInviteCommandMessage() = "`Ru:` Неверная команда для 'invite'"

    override fun localizeAddCrystalsMessage(amount: Int, username: String) = "Успешно добавлено $amount кристаллов пользователю $username"

    override fun localizeUserNotFoundMessage(username: String) = "Пользователь не найден: $username"

    override fun localizeInvalidAmountMessage() = "Некорректное количество"

    override fun localizeAddCrystalsFormatMessage() = "Неправильный формат команды. Используйте: ru?addcry <количество> <пользователь>"

    override fun localizeAddScoreMessage(amount: Int, username: String) = "Успешно добавлено $amount опыта пользователю $username"

    override fun localizeAddScoreFormatMessage() = "Неправильный формат команды. Используйте: ru?addscore <количество> <пользователь>"
}
