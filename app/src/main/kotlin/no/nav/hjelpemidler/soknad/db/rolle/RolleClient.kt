package no.nav.hjelpemidler.soknad.db.rolle

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import no.nav.hjelpemidler.http.correlationId
import no.nav.hjelpemidler.http.createHttpClient
import no.nav.hjelpemidler.http.openid.onBehalfOf
import no.nav.hjelpemidler.http.openid.tokenX

private val log = KotlinLogging.logger { }

class RolleClient(
    engine: HttpClientEngine,
    target: String,
    private val baseUrl: String,
) {
    private val client: HttpClient = createHttpClient(engine) {
        tokenX(target)
        defaultRequest { correlationId() }
    }

    suspend fun hentRolle(userToken: String): RolleResultat {
        val url = "$baseUrl/api/roller"
        return try {
            client
                .get(url) { onBehalfOf(userToken) }
                .body<RolleResultat>()
        } catch (e: Exception) {
            log.error(e) { "Henting av roller feilet" }
            throw e
        }
    }
}
