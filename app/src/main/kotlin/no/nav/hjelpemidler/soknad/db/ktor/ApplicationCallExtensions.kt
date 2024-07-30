package no.nav.hjelpemidler.soknad.db.ktor

import io.ktor.http.RequestConnectionPoint
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.resources.href
import java.util.UUID

val ApplicationCall.søknadId: UUID get() = parameters["soknadId"].let(UUID::fromString)

/**
 * Gjør det mulig å gjøre en "redirect" uten f.eks. HTTP 308 for å endre på URL-er, samtidig som API-et er bakoverkompatibelt.
 *
 * @see <a href="https://stackoverflow.com/questions/60443412/how-to-redirect-internally-in-ktor">How to redirect internally in Ktor?</a>
 */
suspend inline fun <reified T : Any> ApplicationCall.redirectInternally(resource: T) {
    val uri = "/api" + application.href<T>(resource)
    val local = object : RequestConnectionPoint by this.request.local {
        override val uri: String = uri
    }
    val request = object : ApplicationRequest by this.request {
        override val local: RequestConnectionPoint = local
    }
    val call = object : ApplicationCall by this {
        override val request: ApplicationRequest = request
    }
    application.execute(call, Unit)
}
