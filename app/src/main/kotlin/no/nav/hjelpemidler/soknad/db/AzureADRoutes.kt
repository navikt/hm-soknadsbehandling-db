package no.nav.hjelpemidler.soknad.db

import com.fasterxml.jackson.annotation.JsonAlias
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
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.domain.StatusMedÅrsak
import no.nav.hjelpemidler.soknad.db.domain.SøknadData
import no.nav.hjelpemidler.soknad.db.domain.VedtaksresultatData
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.ktor.redirectInternally
import no.nav.hjelpemidler.soknad.db.ktor.søknadId
import no.nav.hjelpemidler.soknad.db.metrics.Metrics
import no.nav.hjelpemidler.soknad.db.sak.HotsakSakId
import no.nav.hjelpemidler.soknad.db.sak.InfotrygdSakId
import no.nav.hjelpemidler.soknad.db.sak.sakApi
import no.nav.hjelpemidler.soknad.db.soknad.Søknader
import no.nav.hjelpemidler.soknad.db.soknad.søknadApi
import no.nav.hjelpemidler.soknad.db.store.Transaction
import java.time.LocalDate
import java.util.UUID

private val logg = KotlinLogging.logger {}

fun Route.azureADRoutes(
    transaction: Transaction,
    metrics: Metrics,
) {
    søknadApi(transaction)
    sakApi(transaction)
    kommuneApi(transaction)

    // fixme -> slettes
    get("/soknad/fnr/{soknadId}") {
        val søknadId = call.søknadId
        val søknad = transaction {
            søknadStore.finnSøknad(søknadId)
        } ?: return@get call.feilmelding(HttpStatusCode.NotFound, "Fant ikke fnr for søknadId: $søknadId")
        call.respond(søknad.fnrBruker)
    }

    post("/soknad/bruker") {
        val søknad = call.receive<SøknadData>()
        logg.info { "Digital behovsmelding mottatt for lagring, søknadId: ${søknad.soknadId}" }
        val rowsUpdated = transaction { søknadStore.lagreBehovsmelding(søknad) }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    post("/ordre") {
        val ordrelinje = call.receive<OrdrelinjeData>()
        logg.info { "Ordrelinje mottatt for lagring, søknadId: ${ordrelinje.søknadId}" }
        val rowsUpdated = transaction { ordreStore.lagre(ordrelinje) }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    post("/soknad/papir") {
        val søknad = call.receive<PapirSøknadData>()
        logg.info { "Papirsøknad mottatt for lagring, søknadId: ${søknad.soknadId}" }
        val rowsUpdated = transaction { søknadStore.lagrePapirsøknad(søknad) }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    // fixme -> slettes
    post("/infotrygd/fagsak") {
        val knytning = call.receive<VedtaksresultatData>()
        logg.info { "Knytter fagsakId: ${knytning.fagsakId} til søknadId: ${knytning.søknadId}" }
        val rowsUpdated = transaction {
            infotrygdStore.lagKnytningMellomSakOgSøknad(
                knytning.søknadId,
                InfotrygdSakId(knytning.fagsakId!!), // fixme
                knytning.fnrBruker,
            )
        }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    // fixme -> slettes
    post("/hotsak/sak") {
        val knytning = call.receive<HotsakTilknytningData>()
        logg.info { "Knytter saksnummer: ${knytning.saksnr} til søknadId: ${knytning.søknadId}" }
        val rowsUpdated = transaction {
            hotsakStore.lagKnytningMellomSakOgSøknad(knytning.søknadId, HotsakSakId(knytning.saksnr))
        }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    // fixme -> slettes
    post("/infotrygd/vedtaksresultat") {
        val vedtaksresultat = call.receive<VedtaksresultatDto>()
        logg.info { "Lagrer vedtaksresultat fra Infotrygd, søknadId: ${vedtaksresultat.søknadId}" }
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

    // fixme -> slettes
    post("/soknad/hotsak/fra-saknummer") {
        data class Request(@JsonAlias("saksnummer") val sakId: HotsakSakId)
        data class Response(val soknadId: UUID?)

        val sakId = call.receive<Request>().sakId
        val søknadId = transaction { hotsakStore.finnSak(sakId) }?.søknadId
        logg.info { "Fant søknadId: $søknadId for sakId: $sakId fra Hotsak" }
        call.respond(Response(søknadId))
    }

    // fixme -> slettes
    post("/soknad/hotsak/har-vedtak/fra-søknadid") {
        data class Request(val søknadId: UUID)
        data class Response(val harVedtak: Boolean)

        val søknadId = call.receive<Request>().søknadId
        val harVedtak = transaction { hotsakStore.finnSak(søknadId) }?.vedtak != null
        logg.info { "Sjekker om søknad med søknadId: $søknadId har vedtak i Hotsak, harVedtak: $harVedtak" }
        call.respond(Response(harVedtak))
    }

    // fixme -> slettes
    post("/hotsak/vedtaksresultat") {
        val vedtaksresultat = call.receive<VedtaksresultatDto>()
        logg.info { "Lagrer vedtaksresultat fra Hotsak, søknadId: ${vedtaksresultat.søknadId}" }
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
        val søknadFinnes = transaction { søknadStore.finnSøknad(søknadId) } != null
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
        val søknad = transaction { søknadStore.hentSøknadData(søknadId) }
        when (søknad) {
            null -> call.feilmelding(HttpStatusCode.NotFound)
            else -> call.respond(søknad)
        }
    }

    post("/soknad/fra-vedtaksresultat") {
        val dto = call.receive<SøknadFraVedtaksresultatDtoV1>()
        val søknadId = transaction {
            infotrygdStore.hentSøknadIdFraVedtaksresultatV1(
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

        data class Response(
            val søknadId: UUID,
            val vedtaksDato: LocalDate?,
        )

        val resultater = transaction {
            infotrygdStore.hentSøknadIdFraVedtaksresultatV2(
                dto.fnrBruker,
                dto.saksblokkOgSaksnr,
            )
        }.map { Response(it.søknadId, it.vedtak?.vedtaksdato) }

        call.respond(resultater)
    }

    // fixme -> slettes
    get("/soknad/opprettet-dato/{soknadId}") {
        val søknadId = call.søknadId
        val opprettetDato = transaction { søknadStore.finnSøknad(søknadId) }?.søknadOpprettet
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

    // fixme -> slettes
    put("/soknad/journalpost-id/{soknadId}") {
        call.redirectInternally(Søknader.SøknadId.Journalpost(Søknader.SøknadId(call.søknadId)))
    }

    // fixme -> slettes
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

    // fixme -> slettes
    get("/soknad/behovsmeldingType/{soknadId}") {
        val søknadId = call.søknadId
        val behovsmeldingType = transaction { søknadStore.finnSøknad(søknadId) }?.behovsmeldingstype
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
