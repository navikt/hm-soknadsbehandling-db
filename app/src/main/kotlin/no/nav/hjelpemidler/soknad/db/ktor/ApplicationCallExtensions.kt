package no.nav.hjelpemidler.soknad.db.ktor

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.response.respondBytesWriter
import io.ktor.util.filter
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyTo
import no.nav.hjelpemidler.server.ktor.security.AutentisertInnbygger
import no.nav.hjelpemidler.server.ktor.security.AuthenticationProviderName
import java.util.UUID

val ApplicationCall.søknadId: UUID get() = parameters["soknadId"].let(UUID::fromString)

data class Response(
    val data: Any,
)

suspend fun ApplicationCall.pipe(response: HttpResponse) {
    val responseBodyChannel: ByteReadChannel = response.bodyAsChannel()
    this.response.headers.let { headers ->
        response.headers
            .filter { key, _ ->
                !key.equals(HttpHeaders.ContentType, ignoreCase = true) &&
                    !key.equals(HttpHeaders.ContentLength, ignoreCase = true) &&
                    !HttpHeaders.isUnsafe(key)
            }
            .forEach { key, value ->
                headers.append(key, value.first())
            }
        // Support use of api in iframes
        headers.append("X-Frame-Options", "SAMEORIGIN")
    }
    this.respondBytesWriter(
        contentType = response.contentType(),
        status = response.status,
        contentLength = response.contentLength(),
    ) {
        // Pipe the body
        responseBodyChannel.copyTo(this)
    }
}

fun ApplicationCall.autentisertInnbygger(): AutentisertInnbygger = principal<AutentisertInnbygger>(
    AuthenticationProviderName.TOKEN_X,
) ?: error("AutentisertInnbygger mangler")
