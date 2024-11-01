package no.nav.hjelpemidler.soknad.db.soknad

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Route
import no.nav.hjelpemidler.behovsmeldingsmodell.Behovsmeldingsgrunnlag
import no.nav.hjelpemidler.behovsmeldingsmodell.Statusendring
import no.nav.hjelpemidler.behovsmeldingsmodell.ordre.Ordrelinje
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Sakstilknytning
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Vedtaksresultat
import no.nav.hjelpemidler.soknad.db.ServiceContext
import no.nav.hjelpemidler.soknad.db.store.Transaction

private val logg = KotlinLogging.logger {}

fun Route.søknadApi(
    transaction: Transaction,
    serviceContext: ServiceContext,
) {
    val søknadService = serviceContext.søknadService

    post<Søknader> {
        val grunnlag = call.receive<Behovsmeldingsgrunnlag>()
        val rowsUpdated = søknadService.lagreBehovsmelding(grunnlag)
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    get<Søknader.SøknadId> {
        val søknadId = it.søknadId
        val søknad = transaction { søknadStore.finnSøknad(søknadId, it.inkluderData) }
        if (søknad == null) {
            logg.info { "Fant ikke søknad med søknadId: $søknadId" }
        }
        call.respondNullable(HttpStatusCode.OK, søknad)
    }

    get<Behovsmelding.BehovsmeldingId> {
        val behovsmeldingId = it.behovsmeldingId
        val behovsmelding = transaction { søknadStore.finnInnsenderbehovsmelding(behovsmeldingId) }
        if (behovsmelding == null) {
            logg.info { "Fant ikke behovsmelding med behovsmeldingId: $behovsmeldingId" }
        }
        call.respondNullable(HttpStatusCode.OK, behovsmelding)
    }

    get<Behovsmelding.BehovsmeldingMetadata> {
        val behovsmeldingId = it.behovsmeldingId
        val behovsmeldingDto = transaction { søknadStore.finnInnsenderbehovsmeldingDto(behovsmeldingId) }
        if (behovsmeldingDto == null) {
            logg.info { "Fant ikke behovsmelding med behovsmeldingId: $behovsmeldingId" }
        }
        call.respondNullable(HttpStatusCode.OK, behovsmeldingDto)
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

    post<Søknader.SøknadId.Ordre> {
        val søknadId = it.parent.søknadId
        val ordrelinje = call.receive<Ordrelinje>()
        val rowsUpdated = transaction { ordreStore.lagre(søknadId, ordrelinje) }
        call.respond(HttpStatusCode.OK, rowsUpdated)
    }

    get<Søknader.SøknadId.Sak> {
        val søknadId = it.parent.søknadId
        val sak = søknadService.finnSak(søknadId)
        if (sak == null) {
            logg.info { "Fant ikke sak for søknadId: $søknadId" }
        }
        call.respondNullable(HttpStatusCode.OK, sak)
    }

    post<Søknader.SøknadId.Sak> {
        val sakstilknytning = call.receive<Sakstilknytning>()
        val rowsUpdated = søknadService.lagreSakstilknytning(it.parent.søknadId, sakstilknytning)
        call.respond(HttpStatusCode.OK, rowsUpdated)
    }

    put<Søknader.SøknadId.Status> {
        val søknadId = it.parent.søknadId
        val statusendring = call.receive<Statusendring>()
        val rowsUpdated = søknadService.oppdaterStatus(søknadId, statusendring)
        call.respond(HttpStatusCode.OK, rowsUpdated)
        serviceContext.metrics.measureElapsedTimeBetweenStatusChanges(søknadId, statusendring.status)
    }

    post<Søknader.SøknadId.Vedtaksresultat> {
        val vedtaksresultat = call.receive<Vedtaksresultat>()
        val rowsUpdated = søknadService.lagreVedtaksresultat(it.parent.søknadId, vedtaksresultat)
        call.respond(HttpStatusCode.OK, rowsUpdated)
    }
}
