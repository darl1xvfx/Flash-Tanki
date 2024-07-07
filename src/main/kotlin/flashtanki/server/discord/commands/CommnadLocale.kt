package flashtanki.server.bot.discord

class RUlocale {
    fun getHelpMessage(): String {
        return """
            **Список команд:**

            `ru?stop` - Останавливает сервер.
            `ru?online` - Показывает количество онлайн игроков и их данные.
            `ru?invite toggle` - Включает или выключает требование инвайт-кода.
            `ru?invite add` <код> - Добавляет новый инвайт-код.
            `ru?invite delete` <код> - Удаляет инвайт-код.
            `ru?invite list` - Список всех инвайт-кодов.
            `ru?invite give` - Генерирует инвайт-код для упомянутого пользователя.
            `ru?addcry` <количество> <пользователь> - Добавляет кристаллы указанному пользователю.
            `ru?addscore` <количество> <пользователь> - Добавляет очки указанному пользователю.
            `ru?help` - Показывает это сообщение.
        """.trimIndent()
    }

    fun serverStopped(memberMention: String?): String {
        return "`Ru:` Сервер остановлен игроком $memberMention"
    }

    fun toggleInvite(enabled: Boolean): String {
        return "`Ru:` Инвайт коды теперь ${if (enabled) "нужны" else "не нужны"} для входа в игру."
    }

    fun inviteAdded(code: String): String {
        return "`Ru:` Инвайт код: `$code`. Был добавлен"
    }

    fun inviteDeleted(deleted: Boolean, code: String): String {
        return if (deleted) "`Ru:` Инвайт `$code` успешно удален." else "Ru: Инвайт `$code` не найдено"
    }

    fun inviteListEntry(code: String, username: String, id: String): String {
        return " - Код: `$code`, Никнейм: `$username` (ID: `$id`)"
    }

    fun noInviteCodes(): String {
        return "`Ru:` Нет доступных пригласительных кодов"
    }

    fun inviteSent(code: String): String {
        return "`Ru:` Твой инвайт код: `$code`"
    }

    fun inviteSentToUser(username: String): String {
        return "`Ru:` Инвайт код успешно отправлен для `$username`."
    }

    fun mentionUserForInvite(): String {
        return "`Ru:` Упомяните пользователя для отправки инвайта."
    }

    fun invalidCommand(): String {
        return "`Ru:` Неверная команда."
    }

    fun crystalsAdded(amount: Int, username: String): String {
        return "Успешно добавлено `$amount` кристаллов пользователю `$username`"
    }

    fun userNotFound(username: String): String {
        return "Пользователь не найден: `$username`"
    }

    fun invalidCrystalsAmount(): String {
        return "Некорректное количество кристаллов"
    }

    fun incorrectAddCryFormat(): String {
        return "Неправильный формат команды. Используйте: `ru?addcry` <количество> <пользователь>"
    }

    fun scoreAdded(amount: Int, username: String): String {
        return "Успешно добавлено `$amount` опыта пользователю `$username`"
    }

    fun invalidScoreAmount(): String {
        return "Некорректное количество опыта"
    }

    fun incorrectAddScoreFormat(): String {
        return "Неправильный формат команды. Используйте: `ru?addscore` <количество> <пользователь>"
    }
}

class ENlocale {
    fun getHelpMessage(): String {
        return """
            **Command List:**

            `en?stop` - Stops the server.
            `en?online` - Displays the number of online players and their details.
            `en?invite toggle` - Toggles the invite code requirement on or off.
            `en?invite add` <code> - Adds a new invite code.
            `en?invite delete` <code> - Deletes an invite code.
            `en?invite list` - Lists all invite codes.
            `en?invite give` - Generates an invite code for a mentioned user.
            `en?addcry` <amount> <username> - Adds crystals to the specified user.
            `en?addscore` <amount> <username> - Adds score to the specified user.
            `en?help` - Displays this help message.
        """.trimIndent()
    }

    fun serverStopped(memberMention: String?): String {
        return "`En:` Server stopped for $memberMention"
    }

    fun toggleInvite(enabled: Boolean): String {
        return "`En:` Invite codes are now ${if (enabled) "enabled" else "not enabled"} to enter the game"
    }

    fun inviteAdded(code: String): String {
        return "`En:` Invite code called: `$code`. Has been added"
    }

    fun inviteDeleted(deleted: Boolean, code: String): String {
        return if (deleted) "`En:` Successfully removed invite code `$code`" else "`En:` Invite `$code` not found"
    }

    fun inviteListEntry(code: String, username: String, id: String): String {
        return " - Code: `$code`, UserName: `$username` (ID: `$id`)"
    }

    fun noInviteCodes(): String {
        return "`En:` No invite codes available"
    }

    fun inviteSent(code: String): String {
        return "`En:` Your Invite Code: `$code`"
    }

    fun inviteSentToUser(username: String): String {
        return "`En:` Invite code successfully sent to `$username`."
    }

    fun mentionUserForInvite(): String {
        return "`En:` Mention the user to send an invite."
    }

    fun invalidCommand(): String {
        return "`En:` Invalid command."
    }

    fun crystalsAdded(amount: Int, username: String): String {
        return "Successfully added `$amount` crystals to user `$username`"
    }

    fun userNotFound(username: String): String {
        return "User not found: `$username`"
    }

    fun invalidCrystalsAmount(): String {
        return "Incorrect amount of crystals"
    }

    fun incorrectAddCryFormat(): String {
        return "Incorrect command format. Use: `en?addcry` <amount> <user>"
    }

    fun scoreAdded(amount: Int, username: String): String {
        return "Successfully added `$amount` score to user `$username`"
    }

    fun invalidScoreAmount(): String {
        return "Incorrect amount of score"
    }

    fun incorrectAddScoreFormat(): String {
        return "Incorrect command format. Use: `en?addscore` <amount> <user>"
    }
}
