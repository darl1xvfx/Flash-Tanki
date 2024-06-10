package flashtanki.server.commands.handlers

import flashtanki.server.commands.*
import flashtanki.server.client.*
import org.koin.core.component.KoinComponent

class ClansHandler : ICommandHandler, KoinComponent {
    @CommandHandler(CommandName.ShowClanServer)
    suspend fun showClanServer(socket: UserSocket) {
        Command(CommandName.ShowClanClient).send(socket)
    }
}