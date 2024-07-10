package no.nav.hjelpemidler.soknad.db.ktor

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import java.util.UUID

val ApplicationCall.søknadId: UUID get() = parameters["soknadId"].let(UUID::fromString)

fun ApplicationCall.permanentRedirect(url: String) {
    response.header(HttpHeaders.Location, "/api$url")
    response.status(HttpStatusCode.PermanentRedirect)
}
