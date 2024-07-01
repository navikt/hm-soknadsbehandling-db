package no.nav.hjelpemidler.soknad.db.rolle

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.hjelpemidler.soknad.db.Configuration
import no.nav.hjelpemidler.soknad.db.httpClient
import no.nav.tms.token.support.tokendings.exchange.TokendingsService

private val logger = KotlinLogging.logger { }

class RolleClient(
    private val tokendingsService: TokendingsService,
    private val url: String = Configuration.application.hmRollerUrl,
    private val audience: String = Configuration.application.hmRollerAudience,
) {
    private val client: HttpClient = httpClient()

    suspend fun hentRolle(token: String): RolleResultat {
        return try {
            withContext(Dispatchers.IO) {
                val exchangedToken = tokendingsService.exchangeToken(token, audience)
                client.get("$url/api/roller") { bearerAuth(exchangedToken) }.body<RolleResultat>()
            }
        } catch (e: Exception) {
            logger.error(e) { "Henting av roller feilet" }
            throw e
        }
    }
}
