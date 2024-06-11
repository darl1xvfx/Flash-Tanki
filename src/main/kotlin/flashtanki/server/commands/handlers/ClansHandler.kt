package flashtanki.server.commands.handlers

import flashtanki.server.commands.*
import flashtanki.server.client.*
import flashtanki.server.lobby.clan.*
import org.koin.core.component.inject
import org.koin.core.component.KoinComponent

class ClansHandler : ICommandHandler, KoinComponent {

    private val clanRepository: IClanRepository by inject()

    @CommandHandler(CommandName.ShowClanServer)
    suspend fun showClanServer(socket: UserSocket) {
        Command(CommandName.ShowClanClient).send(socket)
    }

    @CommandHandler(CommandName.ClanValidateTag)
    suspend fun clanValidateTag(socket: UserSocket, tag: String) {
        if(clanRepository.getClanByTag(tag) != null) {
            Command(CommandName.СlanTagExist).send(socket)
            return
        }
        Command(CommandName.СlanTagNotExist).send(socket)
    }

    @CommandHandler(CommandName.ClanValidateName)
    suspend fun clanValidateName(socket: UserSocket, name: String) {
        if(clanRepository.getClanByName(name) != null) {
            Command(CommandName.СlanNameExist).send(socket)
            return
        }
        Command(CommandName.СlanNameNotExist).send(socket)
    }

    @CommandHandler(CommandName.ClanCreateServer)
    suspend fun clanCreate(socket: UserSocket, name: String, tag: String)
    {
        val clan = clanRepository.createClan(tag, name, socket.user!!.username)
    }
}