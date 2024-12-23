package no.nav.hjelpemidler.soknad.db.rolle

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.hjelpemidler.http.createHttpClient
import no.nav.hjelpemidler.soknad.db.Configuration
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder

private val logg = KotlinLogging.logger { }

class RolleClient(
    private val url: String = Configuration.HM_ROLLER_URL,
    private val audience: String = Configuration.HM_ROLLER_AUDIENCE,
) {
    private val tokendingsService = TokendingsServiceBuilder.buildTokendingsService()
    private val client: HttpClient = createHttpClient(Apache.create())

    suspend fun hentRolle(token: String): RolleResultat {
        return try {
            withContext(Dispatchers.IO) {
                val exchangedToken = tokendingsService.exchangeToken(token, audience)
                client.get("$url/api/roller") { bearerAuth(exchangedToken) }.body<RolleResultat>()
            }
        } catch (e: Exception) {
            logg.error(e) { "Henting av roller feilet" }
            throw e
        }
    }
}
