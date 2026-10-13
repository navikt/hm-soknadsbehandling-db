package no.nav.hjelpemidler.soknad.db

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.store.Transaction
import java.util.UUID

private val log = KotlinLogging.logger {}

fun Route.kommuneApi() {
    val transaction: Transaction by application.dependencies

    post<KommuneApi.Søknader> {
        data class Request(
            val kommunenummer: String,
            val nyereEnn: UUID?,
            val nyereEnnTidsstempel: Long?,
            val nyDatamodell: Boolean?,
        ) {
            fun isValid() = kommunenummer.isNotEmpty() && (kommunenummer.toIntOrNull()?.let { it in 0..9999 } ?: false)
        }

        val request = call.receive<Request>()
        if (!request.isValid()) {
            call.feilmelding(HttpStatusCode.BadRequest, "Ugyldig request: $request")
            return@post
        }

        log.info { "Henter søknader for kommune-API-et, kommunenummer: ${request.kommunenummer}" }
        val søknader = transaction {
            søknadStore.hentBehovsmeldingerForKommuneApiet(
                kommunenummer = request.kommunenummer,
                nyereEnn = request.nyereEnn,
                nyereEnnTidsstempel = request.nyereEnnTidsstempel,
            )
        }
        call.respond(søknader)
    }
}

@Resource("/kommune-api")
class KommuneApi {
    @Resource("/soknader")
    class Søknader(val parent: KommuneApi = KommuneApi())
}
