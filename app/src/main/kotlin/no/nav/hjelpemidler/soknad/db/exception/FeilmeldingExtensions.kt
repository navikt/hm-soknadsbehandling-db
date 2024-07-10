package no.nav.hjelpemidler.soknad.db.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import no.nav.hjelpemidler.configuration.Environment

private val logg = KotlinLogging.logger {}

fun Application.feilmelding() {
    install(StatusPages) {
        // 400
        exception<BadRequestException> { call, cause ->
            logg.error(cause) { cause.melding }
            call.feilmelding(cause, HttpStatusCode.BadRequest)
        }
        // 404
        exception<NotFoundException> { call, cause ->
            logg.error(cause) { cause.melding }
            call.feilmelding(cause, HttpStatusCode.NotFound)
        }
        // 501
        exception<NotImplementedError> { call, cause ->
            logg.error(cause) { cause.melding }
            call.feilmelding(cause, HttpStatusCode.NotImplemented)
        }
        // 500
        exception<Throwable> { call, cause ->
            logg.error(cause) { cause.melding }
            call.feilmelding(cause, HttpStatusCode.InternalServerError)
        }
    }
}

suspend fun ApplicationCall.feilmelding(feilmelding: Feilmelding) =
    respond(feilmelding.status, feilmelding)

suspend fun ApplicationCall.feilmelding(status: HttpStatusCode, message: String? = null) =
    feilmelding(Feilmelding(call = this, status = status, message = message))

suspend fun ApplicationCall.feilmelding(cause: Throwable, status: HttpStatusCode, message: String? = null) =
    feilmelding(Feilmelding(call = this, cause = cause, status = status, message = message))

val Throwable?.melding get() = this?.message ?: "Ukjent feil"

val Throwable?.trace: String?
    get() = if (Environment.current.tier.isProd) {
        null
    } else {
        this?.stackTraceToString()
    }
