package no.nav.hjelpemidler.soknad.db.rapportering.epost

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.hjelpemidler.http.createHttpClient
import no.nav.hjelpemidler.http.openid.entraId

private val log = KotlinLogging.logger { }

class GraphClient(
    engine: HttpClientEngine,
    private val client: HttpClient = createHttpClient(engine) {
        expectSuccess = true
        entraId("https://graph.microsoft.com/.default")
        defaultRequest {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
    },
    private val baseUrl: String = "https://graph.microsoft.com/v1.0",
) {
    suspend fun sendEpost(epostRequest: SendMailRequest, avsender: String) {
        try {
            client.post("$baseUrl/users/$avsender/sendMail") {
                setBody(epostRequest)
            }
        } catch (e: Exception) {
            log.error(e) { "Sending av epost feilet for: $epostRequest, avsender: $avsender" }
            throw e
        }
    }
}
