package flashtanki.server.bot.discord

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*

class OAuthService() {
    fun init()
    {
        embeddedServer(Netty, port = 7777) {
            install(Authentication) {
                oauth("discord-oauth") {
                    client = HttpClient(CIO)
                    providerLookup = { OAuthServerSettings.OAuth2ServerSettings(
                        name = "discord",
                        authorizeUrl = "https://discord.com/api/oauth2/authorize",
                        accessTokenUrl = "https://discord.com/api/oauth2/token",
                        clientId = "1248976181466890271",
                        clientSecret = "uMgbC3CEdO2iVAKm7jLBvgLX3TRzBfrv",
                        defaultScopes = listOf("identify")
                    ) }
                    urlProvider = { "http://localhost:7777/login" }
                }
            }

            routing {
                authenticate("discord-oauth") {
                    get("/login") {
                        val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                    }
                }
            }
        }.start()
    }
}
