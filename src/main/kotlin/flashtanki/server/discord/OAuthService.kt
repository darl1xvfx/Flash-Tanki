package flashtanki.server.bot.discord

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.auth.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent

class OAuthService : KoinComponent {
    private val logger = KotlinLogging.logger {}
    private val httpClient = HttpClient(CIO)

    fun init() {
        embeddedServer(Netty, port = 7777) {
            install(Authentication) {
                oauth("discord-oauth") {
                    client = httpClient
                    providerLookup = {
                        OAuthServerSettings.OAuth2ServerSettings(
                            name = "discord",
                            authorizeUrl = "https://discord.com/api/oauth2/authorize",
                            accessTokenUrl = "https://discord.com/api/oauth2/token",
                            clientId = "1249282768786030653",
                            clientSecret = "n_MDYqC6v7mhkhk7U2DQCOVqNoFxX4Qv",
                            defaultScopes = listOf("identify")
                        )
                    }
                    urlProvider = { "http://localhost:7777/api/oauth/discord/redirect" }
                }
            }

            routing {
                authenticate("discord-oauth") {
                    get("/oauth/discord/redirect") {
                        val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                        if (principal != null) {
                            call.respondText("Authentication successful")
                        } else {
                            call.respondText("Authentication failed")
                        }
                    }
                }

                get("/start") {
                    val authorizationUrl = "https://discord.com/oauth2/authorize?" +
                            "client_id=1249282768786030653" +
                            "&response_type=code" +
                            "&redirect_uri=http%3A%2F%2Flocalhost%3A7777%2Fapi%2Foauth%2Fdiscord%2Fredirect" +
                            "&scope=identify+gdm.join+guilds+email+connections+guilds.join"

                    call.respondRedirect(authorizationUrl)
                }
            }
        }.start()
        logger.debug { "OAuthService initialized!" }
    }

    suspend fun getUserInfo(accessToken: String): String {
        val response = httpClient.get("https://discord.com/api/users/@me") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
                append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            }
        }
        return response.body<String>()
    }
}
