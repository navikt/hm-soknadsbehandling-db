package no.nav.hjelpemidler.soknad.db.rapportering.epost

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.hjelpemidler.http.createHttpClient
import no.nav.hjelpemidler.http.openid.OpenIDClient
import no.nav.hjelpemidler.http.openid.bearerAuth

private val log = KotlinLogging.logger { }

class GraphClient(
    private val openIDClient: OpenIDClient,
    private val httpClient: HttpClient = createHttpClient(Apache.create()) {
        expectSuccess = true

        // TODO fjern logging
        install(Logging) {
            level = LogLevel.BODY
        }

        defaultRequest {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
    },
    private val baseUrl: String = "https://graph.microsoft.com/v1.0",
    private val scope: String = "https://graph.microsoft.com/.default",
) {

    suspend fun sendEpost(request: SendMailRequest, avsender: String) {
        try {
            withContext(Dispatchers.IO) {
                val tokenSet = openIDClient.grant(scope)
                httpClient.post("$baseUrl/users/$avsender/sendMail") {
                    bearerAuth(tokenSet)
                    setBody(request)
                }
            }
        } catch (t: Throwable) {
            log.error(t) { "Sending av epost feilet for $request, avsender=$avsender" }
            throw t
        }
    }
}
