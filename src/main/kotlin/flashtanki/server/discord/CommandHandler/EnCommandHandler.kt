package flashtanki.server.discord.CommandHandler

class EnCommandHandler : BaseCommandHandler(prefix = "en?") {
    override fun buildHelpMessage(): String {
        return """
            **Command List:**

            `en?stop` - Stops the server.
            `en?online` - Displays the number of online players and their details.
            `en?invite toggle` - Toggles the invite code requirement on or off.
            `en?invite add <code>` - Adds a new invite code.
            `en?invite delete <code>` - Deletes an invite code.
            `en?invite list` - Lists all invite codes.
            `en?invite give` - Generates an invite code for a mentioned user.
            `en?addcry <amount> <username>` - Adds crystals to the specified user.
            `en?addscore <amount> <username>` - Adds score to the specified user.
            `en?help` - Displays this help message.
        """.trimIndent()
    }

    override fun localizeStopMessage(mention: String) = "`En:` Server stopped by $mention!"

    override fun localizeOnlineMessageHeader(playerCount: Int) = "__**Online**__: $playerCount\n"

    override fun localizePlayerListMessage(players: String) = "__**Players**__: $players\n"

    override fun localizeNoPlayersMessage() = "None"

    override fun localizeScreenMessage(screenName: String, players: String) = "__**Players in $screenName**__: $players\n"

    override fun localizeInviteToggleMessage(enabled: Boolean) = if (enabled) "`En:` Invite codes are now `enabled` to enter the game" else "`En:` Invite codes are now `not enabled` to enter the game"

    override fun localizeInviteAddMessage(code: String) = "`En:` Invite code: $code has been added."

    override fun localizeInviteDeleteMessage(code: String, deleted: Boolean) = if (deleted) "`En:` Successfully removed invite code '$code'" else "`En:` Invite '$code' not found"

    override fun localizeNoInviteCodesMessage() = "`En:` No invite codes available."

    override fun localizeInviteSendMessage(code: String) = "`En:` Your Invite Code: `$code`"

    override fun localizeInviteSuccessMessage(username: String) = "`En:` Invite code successfully sent to `$username`."

    override fun localizeMentionUserMessage() = "`En:` Mention the user to send an invite."

    override fun localizeInvalidInviteCommandMessage() = "`En:` Invalid command for 'invite'"

    override fun localizeAddCrystalsMessage(amount: Int, username: String) = "Successfully added $amount crystals to $username"

    override fun localizeUserNotFoundMessage(username: String) = "User not found: $username"

    override fun localizeInvalidAmountMessage() = "Invalid amount"

    override fun localizeAddCrystalsFormatMessage() = "Incorrect command format. Use: en?addcry <amount> <username>"

    override fun localizeAddScoreMessage(amount: Int, username: String) = "Successfully added $amount score to $username"

    override fun localizeAddScoreFormatMessage() = "Incorrect command format. Use: en?addscore <amount> <username>"
}
