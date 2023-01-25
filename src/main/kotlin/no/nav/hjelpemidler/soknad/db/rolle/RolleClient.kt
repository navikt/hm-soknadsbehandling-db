package no.nav.hjelpemidler.soknad.db.rolle

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.hjelpemidler.soknad.db.Configuration
import no.nav.hjelpemidler.soknad.db.httpClient
import no.nav.tms.token.support.tokendings.exchange.TokendingsService

private val logger = KotlinLogging.logger { }

class RolleClient(
    private val tokendingsService: TokendingsService,
    private val client: HttpClient = httpClient(),
    private val url: String = Configuration.application.hmRollerUrl,
    private val audience: String = Configuration.application.hmRollerAudience,
) {
    suspend fun hentRolle(token: String): RolleResultat {
        val exchangedToken = tokendingsService.exchangeToken(token, audience)

        return try {
            withContext(Dispatchers.IO) {
                client.get("$url/api/roller") {
                    headers {
                        header("Authorization", "Bearer $exchangedToken")
                    }
                }.body()
            }
        } catch (e: Exception) {
            logger.error(e) { "Henting av rolle feilet" }
            throw e
        }
    }
}
