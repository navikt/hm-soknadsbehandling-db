package no.nav.hjelpemidler.soknad.db

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.metrics.Metrics
import no.nav.hjelpemidler.soknad.db.resources.Søknader
import no.nav.hjelpemidler.soknad.db.store.Transaction

private val logg = KotlinLogging.logger {}

fun Route.søknadApi(
    transaction: Transaction,
    metrics: Metrics,
) {
    get<Søknader.SøknadId> {
        val søknad = transaction {
            søknadStore.finnSøknad(it.søknadId)
        } ?: return@get call.feilmelding(HttpStatusCode.NotFound, "Fant ikke søknad med søknadId: ${it.søknadId}")
        call.respond(søknad)
    }

    put<Søknader.SøknadId.Journalpost> {
        data class Request(val journalpostId: String)

        val søknadId = it.parent.søknadId
        val journalpostId = call.receive<Request>().journalpostId
        logg.info { "Knytter journalpostId: $journalpostId til søknadId: $søknadId" }
        val rowsUpdated = transaction { søknadStore.oppdaterJournalpostId(søknadId, journalpostId) }
        call.respond(rowsUpdated)
    }

    put<Søknader.SøknadId.Oppgave> {
        data class Request(val oppgaveId: String)

        val søknadId = it.parent.søknadId
        val oppgaveId = call.receive<Request>().oppgaveId
        logg.info { "Knytter oppgaveId: $oppgaveId til søknadId: $søknadId" }
        val rowsUpdated = transaction { søknadStore.oppdaterOppgaveId(søknadId, oppgaveId) }
        call.respond(rowsUpdated)
    }
}
