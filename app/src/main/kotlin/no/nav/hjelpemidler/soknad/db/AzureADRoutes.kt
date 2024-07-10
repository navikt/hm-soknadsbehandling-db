package no.nav.hjelpemidler.soknad.db

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import no.nav.hjelpemidler.soknad.db.domain.BehovsmeldingType
import no.nav.hjelpemidler.soknad.db.domain.HotsakTilknytningData
import no.nav.hjelpemidler.soknad.db.domain.OrdrelinjeData
import no.nav.hjelpemidler.soknad.db.domain.PapirSøknadData
import no.nav.hjelpemidler.soknad.db.domain.SoknadData
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.domain.StatusMedÅrsak
import no.nav.hjelpemidler.soknad.db.domain.VedtaksresultatData
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.ktor.redirectInternally
import no.nav.hjelpemidler.soknad.db.ktor.søknadId
import no.nav.hjelpemidler.soknad.db.metrics.Metrics
import no.nav.hjelpemidler.soknad.db.resources.Søknader
import no.nav.hjelpemidler.soknad.db.store.Transaction
import java.util.UUID

private val logg = KotlinLogging.logger {}

fun Route.azureADRoutes(
    transaction: Transaction,
    metrics: Metrics,
) {
    søknadApi(transaction, metrics)
    kommuneApi(transaction)

    get("/soknad/fnr/{soknadId}") {
        val søknadId = call.søknadId
        val fnr = transaction { søknadStore.hentFnrForSøknad(søknadId) }
        call.respond(fnr)
    }

    post("/soknad/bruker") {
        val søknad = call.receive<SoknadData>()
        logg.info { "Digital behovsmelding mottatt for lagring, søknadId: ${søknad.soknadId}" }
        val rowsUpdated = transaction { søknadStore.save(søknad) }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    post("/ordre") {
        val ordrelinje = call.receive<OrdrelinjeData>()
        logg.info { "Ordrelinje mottatt for lagring, søknadId: ${ordrelinje.søknadId}" }
        val rowsUpdated = transaction { ordreStore.save(ordrelinje) }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    post("/soknad/papir") {
        val søknad = call.receive<PapirSøknadData>()
        logg.info { "Papirsøknad mottatt for lagring, søknadId: ${søknad.soknadId}" }
        val rowsUpdated = transaction { søknadStore.savePapir(søknad) }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    post("/infotrygd/fagsak") {
        val knytning = call.receive<VedtaksresultatData>()
        logg.info { "Knytter fagsakId: ${knytning.fagsakId} til søknadId: ${knytning.søknadId}" }
        val rowsUpdated = transaction { infotrygdStore.lagKnytningMellomFagsakOgSøknad(knytning) }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    post("/hotsak/sak") {
        val knytning = call.receive<HotsakTilknytningData>()
        logg.info { "Knytter saksnummer: ${knytning.saksnr} til søknadId: ${knytning.søknadId}" }
        val rowsUpdated = transaction { hotsakStore.lagKnytningMellomSakOgSøknad(knytning) }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    post("/infotrygd/vedtaksresultat") {
        val vedtaksresultat = call.receive<VedtaksresultatDto>()
        logg.info { "Lagrer vedtaksresultat fra Infotrygd: ${vedtaksresultat.søknadId}" }
        val rowsUpdated = transaction {
            infotrygdStore.lagreVedtaksresultat(
                vedtaksresultat.søknadId,
                vedtaksresultat.vedtaksresultat,
                vedtaksresultat.vedtaksdato,
                vedtaksresultat.soknadsType,
            )
        }
        call.respond(rowsUpdated)
    }

    get("/infotrygd/søknadsType/{soknadId}") {
        val søknadId = call.søknadId
        val søknadstype = transaction { infotrygdStore.hentTypeForSøknad(søknadId) }

        data class Response(val søknadsType: String?)
        call.respond(Response(søknadstype))
    }

    post("/soknad/hotsak/fra-saknummer") {
        data class Request(val saksnummer: String)
        data class Response(val soknadId: UUID?)

        val saksnummer = call.receive<Request>().saksnummer
        val søknadId = transaction { hotsakStore.hentSøknadsIdForHotsakNummer(saksnummer) }
        logg.info { "Fant søknadId: $søknadId for saksnummer: $saksnummer fra Hotsak" }
        call.respond(Response(søknadId))
    }

    post("/soknad/hotsak/har-vedtak/fra-søknadid") {
        data class Request(val søknadId: UUID)
        data class Response(val harVedtak: Boolean)

        val søknadId = call.receive<Request>().søknadId
        val harVedtak = transaction { hotsakStore.harVedtakForSøknadId(søknadId) }
        logg.info { "Sjekker om søknad med søknadId: $søknadId har vedtak i Hotsak, harVedtak: $harVedtak" }
        call.respond(Response(harVedtak))
    }

    post("/hotsak/vedtaksresultat") {
        val vedtaksresultat = call.receive<VedtaksresultatDto>()
        logg.info { "Lagrer vedtaksresultat fra Hotsak: ${vedtaksresultat.søknadId}" }
        val rowsUpdated = transaction {
            hotsakStore.lagreVedtaksresultat(
                vedtaksresultat.søknadId,
                vedtaksresultat.vedtaksresultat,
                vedtaksresultat.vedtaksdato,
            )
        }
        call.respond(rowsUpdated)
    }

    delete("/soknad/bruker") {
        val søknadId = call.receive<UUID>()
        logg.info { "Sletter søknad med søknadId: $søknadId" }
        val rowsDeleted = transaction { søknadStore.slettSøknad(søknadId) }
        call.respond(rowsDeleted)
    }

    delete("/soknad/utlopt/bruker") {
        val søknadId = call.receive<UUID>()
        logg.info { "Sletter utløpt søknad med søknadId: $søknadId" }
        val rowsDeleted = transaction { søknadStore.slettUtløptSøknad(søknadId) }
        call.respond(rowsDeleted)
    }

    put("/soknad/status/{soknadId}") {
        val søknadId = call.søknadId
        val nyStatus = call.receive<Status>()
        logg.info { "Oppdaterer status på søknad med søknadId: $søknadId, nyStatus: $nyStatus" }
        val rowsUpdated = transaction { søknadStore.oppdaterStatus(søknadId, nyStatus) }
        call.respond(rowsUpdated)
        metrics.measureElapsedTimeBetweenStatusChanges(søknadId, nyStatus)
    }

    put("/soknad/statusV2") {
        val statusMedÅrsak = call.receive<StatusMedÅrsak>()
        logg.info { "Oppdaterer status på søknad med søknadId: ${statusMedÅrsak.søknadId}, nyStatus: ${statusMedÅrsak.status} (v2)" }
        val rowsUpdated = transaction { søknadStore.oppdaterStatusMedÅrsak(statusMedÅrsak) }
        call.respond(rowsUpdated)
        metrics.measureElapsedTimeBetweenStatusChanges(statusMedÅrsak.søknadId, statusMedÅrsak.status)
    }

    get("/soknad/bruker/finnes/{soknadId}") {
        val søknadId = call.søknadId
        val søknadFinnes = transaction { søknadStore.søknadFinnes(søknadId) }
        call.respond("soknadFinnes" to søknadFinnes)
    }

    // NB! Skrivefeil i denne.
    post("/infotrygd/fnr-jounralpost") {
        data class Request(
            val fnrBruker: String,
            val journalpostId: Int,
        )

        val fnrOgJournalpostIdFinnesDto = call.receive<Request>()
        val fnrOgJournalpostIdFinnes = transaction {
            søknadStore.fnrOgJournalpostIdFinnes(
                fnrOgJournalpostIdFinnesDto.fnrBruker,
                fnrOgJournalpostIdFinnesDto.journalpostId,
            )
        }
        call.respond("fnrOgJournalpostIdFinnes" to fnrOgJournalpostIdFinnes)
    }

    get("/soknadsdata/bruker/{soknadId}") {
        val søknadId = call.søknadId
        val søknad = transaction { søknadStore.hentSoknadData(søknadId) }
        when (søknad) {
            null -> call.feilmelding(HttpStatusCode.NotFound)
            else -> call.respond(søknad)
        }
    }

    post("/soknad/fra-vedtaksresultat") {
        val dto = call.receive<SøknadFraVedtaksresultatDtoV1>()
        val søknadId = transaction {
            infotrygdStore.hentSøknadIdFraVedtaksresultat(
                dto.fnrBruker,
                dto.saksblokkOgSaksnr,
                dto.vedtaksdato,
            )
        }

        data class Response(val soknadId: UUID?)
        call.respond(Response(søknadId))
    }

    post("/soknad/fra-vedtaksresultat-v2") {
        val dto = call.receive<SøknadFraVedtaksresultatDtoV2>()
        val resultater = transaction {
            infotrygdStore.hentSøknadIdFraVedtaksresultatV2(
                dto.fnrBruker,
                dto.saksblokkOgSaksnr,
            )
        }
        call.respond(resultater)
    }

    get("/soknad/opprettet-dato/{soknadId}") {
        val søknadId = call.søknadId
        val opprettetDato = transaction { søknadStore.hentSoknadOpprettetDato(søknadId) }
        when (opprettetDato) {
            null -> call.feilmelding(HttpStatusCode.NotFound)
            else -> call.respond(opprettetDato)
        }
    }

    get("/soknad/utgaatt/{dager}") {
        val dager = call.parameters["dager"]?.toInt() ?: throw BadRequestException("Parameter 'dager' var ugyldig")
        val søknader = transaction { søknadStore.hentSøknaderTilGodkjenningEldreEnn(dager) }
        call.respond(søknader)
    }

    put("/soknad/journalpost-id/{soknadId}") {
        call.redirectInternally(Søknader.SøknadId.Journalpost(Søknader.SøknadId(call.søknadId)))
    }

    put("/soknad/oppgave-id/{soknadId}") {
        call.redirectInternally(Søknader.SøknadId.Oppgave(Søknader.SøknadId(call.søknadId)))
    }

    get("/soknad/ordre/ordrelinje-siste-doegn/{soknadId}") {
        val søknadId = call.søknadId
        val result = transaction { ordreStore.ordreSisteDøgn(søknadId) }
        call.respond(result)
    }

    get("/soknad/ordre/har-ordre/{soknadId}") {
        val søknadId = call.søknadId
        val result = transaction { ordreStore.harOrdre(søknadId) }
        call.respond(result)
    }

    get("/soknad/behovsmeldingType/{soknadId}") {
        val søknadId = call.søknadId
        val behovsmeldingType = transaction { søknadStore.behovsmeldingTypeFor(søknadId) }
        logg.info {
            when (behovsmeldingType) {
                null -> "Kunne ikke finne behovsmeldingType for søknadId: $søknadId"
                else -> "Fant behovsmeldingType: $behovsmeldingType for søknadId: $søknadId"
            }
        }
        data class Result(val behovsmeldingType: BehovsmeldingType?)
        call.respond(Result(behovsmeldingType))
    }

    get("/forslagsmotor/tilbehoer/datasett") {
        logg.info { "Henter initielt datasett til forslagsmotoren for tilbehør" }
        val result = transaction {
            søknadStore.hentInitieltDatasettForForslagsmotorTilbehør()
        }
        call.respond(result)
    }
}