package no.nav.hjelpemidler.soknad.db.soknad

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Sakstilknytning
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Vedtaksresultat
import no.nav.hjelpemidler.soknad.db.ServiceContext
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.store.Transaction

private val logg = KotlinLogging.logger {}

fun Route.søknadApi(
    transaction: Transaction,
    serviceContext: ServiceContext,
) {
    val søknadService = serviceContext.søknadService

    get<Søknader.SøknadId> {
        val søknad = transaction {
            søknadStore.finnSøknad(it.søknadId, it.inkluderData)
        } ?: return@get call.feilmelding(HttpStatusCode.NotFound, "Fant ikke søknad med søknadId: ${it.søknadId}")
        call.respond(HttpStatusCode.OK, søknad)
    }

    put<Søknader.SøknadId.Journalpost> {
        data class Journalpost(val journalpostId: String)

        val søknadId = it.parent.søknadId
        val journalpostId = call.receive<Journalpost>().journalpostId
        logg.info { "Knytter journalpostId: $journalpostId til søknadId: $søknadId" }
        val rowsUpdated = transaction { søknadStore.oppdaterJournalpostId(søknadId, journalpostId) }
        call.respond(HttpStatusCode.OK, rowsUpdated)
    }

    put<Søknader.SøknadId.Oppgave> {
        data class Oppgave(val oppgaveId: String)

        val søknadId = it.parent.søknadId
        val oppgaveId = call.receive<Oppgave>().oppgaveId
        logg.info { "Knytter oppgaveId: $oppgaveId til søknadId: $søknadId" }
        val rowsUpdated = transaction { søknadStore.oppdaterOppgaveId(søknadId, oppgaveId) }
        call.respond(HttpStatusCode.OK, rowsUpdated)
    }

    get<Søknader.SøknadId.Sak> {
        val søknadId = it.parent.søknadId
        val sak = søknadService.finnSak(søknadId) ?: return@get call.feilmelding(
            HttpStatusCode.NotFound,
            "Fant ikke sak for søknadId: $søknadId",
        )
        call.respond(HttpStatusCode.OK, sak)
    }

    post<Søknader.SøknadId.Sak> {
        val sakstilknytning = call.receive<Sakstilknytning>()
        val rowsUpdated = søknadService.lagreSakstilknytning(it.parent.søknadId, sakstilknytning)
        call.respond(HttpStatusCode.OK, rowsUpdated)
    }

    put<Søknader.SøknadId.Status> {
        data class Statusendring(
            val status: BehovsmeldingStatus,
            val valgteÅrsaker: Set<String>?,
            val begrunnelse: String?,
        )

        val søknadId = it.parent.søknadId
        val statusendring = call.receive<Statusendring>()

        logg.info { "Oppdaterer status på søknad, søknadId: $søknadId, status: ${statusendring.status}" }

        val rowsUpdated = transaction {
            søknadStore.oppdaterStatus(
                søknadId,
                statusendring.status,
                statusendring.valgteÅrsaker,
                statusendring.begrunnelse,
            )
        }
        call.respond(HttpStatusCode.OK, rowsUpdated)
        serviceContext.metrics.measureElapsedTimeBetweenStatusChanges(søknadId, statusendring.status)
    }

    post<Søknader.SøknadId.Vedtaksresultat> {
        val vedtaksresultat = call.receive<Vedtaksresultat>()
        val rowsUpdated = søknadService.lagreVedtaksresultat(it.parent.søknadId, vedtaksresultat)
        call.respond(HttpStatusCode.OK, rowsUpdated)
    }
}
