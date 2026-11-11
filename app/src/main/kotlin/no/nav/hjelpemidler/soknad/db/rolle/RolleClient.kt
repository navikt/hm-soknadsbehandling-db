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
import java.io.Closeable

private val log = KotlinLogging.logger { }

class RolleClient(
    engine: HttpClientEngine,
    target: String,
    private val baseUrl: String,
) : Closeable {
    private val client: HttpClient = createHttpClient(engine) {
        tokenX(target)
        defaultRequest { correlationId() }
    }

    suspend fun hentRoller(userToken: String): RollerResultat {
        val url = "$baseUrl/api/v2/roller"
        return try {
            client
                .get(url) { onBehalfOf(userToken) }
                .body<RollerResultat>()
        } catch (e: Exception) {
            log.error(e) { "Henting av roller feilet" }
            throw e
        }
    }

    override fun close() {
        client.close()
    }
}
