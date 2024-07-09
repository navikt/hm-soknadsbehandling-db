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
import no.nav.hjelpemidler.soknad.db.ktor.søknadId
import no.nav.hjelpemidler.soknad.db.metrics.Metrics
import no.nav.hjelpemidler.soknad.db.store.Transaction
import java.util.UUID

private val logg = KotlinLogging.logger {}

fun Route.azureADRoutes(
    transaction: Transaction,
    metrics: Metrics,
) {
    get("/soknad/fnr/{soknadId}") {
        try {
            val søknadId = call.søknadId
            val fnrForSoknad = transaction { søknadStore.hentFnrForSoknad(søknadId) }
            call.respond(fnrForSoknad)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved henting av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad")
        }
    }

    post("/soknad/bruker") {
        try {
            val soknadToBeSaved = call.receive<SoknadData>()
            transaction { søknadStore.save(soknadToBeSaved) }
            call.respond("OK")
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved lagring av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av søknad")
        }
    }

    post("/ordre") {
        try {
            val ordreToBeSaved = call.receive<OrdrelinjeData>()
            val rowsUpdated = transaction { ordreStore.save(ordreToBeSaved) }
            call.respond(rowsUpdated)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved lagring av ordrelinje" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av ordrelinje")
        }
    }

    post("/soknad/papir") {
        try {
            val papirsoknadToBeSaved = call.receive<PapirSøknadData>()
            val rowsUpdated = transaction { søknadStore.savePapir(papirsoknadToBeSaved) }
            call.respond(rowsUpdated)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved lagring av papirsøknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av papirsøknad")
        }
    }

    post("/infotrygd/fagsak") {
        try {
            val vedtaksresultatData = call.receive<VedtaksresultatData>()
            val numRows = transaction { infotrygdStore.lagKnytningMellomFagsakOgSøknad(vedtaksresultatData) }
            call.respond(numRows)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved lagring av ordrelinje" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av ordrelinje")
        }
    }

    post("/hotsak/sak") {
        try {
            val hotsakTilknytningData = call.receive<HotsakTilknytningData>()
            val numRows = transaction { hotsakStore.lagKnytningMellomSakOgSøknad(hotsakTilknytningData) }
            call.respond(numRows)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved lagring av hotsak-tilknytning" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av hotsak-tilknytning")
        }
    }

    post("/infotrygd/vedtaksresultat") {
        try {
            val vedtaksresultatToBeSaved = call.receive<VedtaksresultatDto>()
            val rowUpdated = transaction {
                infotrygdStore.lagreVedtaksresultat(
                    vedtaksresultatToBeSaved.søknadId,
                    vedtaksresultatToBeSaved.vedtaksresultat,
                    vedtaksresultatToBeSaved.vedtaksdato,
                    vedtaksresultatToBeSaved.soknadsType,
                )
            }
            call.respond(rowUpdated)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved lagring av vedtaksresultat" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av vedtaksresultat")
        }
    }

    get("/infotrygd/søknadsType/{soknadId}") {
        val søknadId = call.søknadId
        val søknadsType = transaction { infotrygdStore.hentTypeForSøknad(søknadId) }

        data class Response(val søknadsType: String?)
        call.respond(Response(søknadsType))
    }

    post("/soknad/hotsak/fra-saknummer") {
        try {
            val soknadFraHotsakNummerDto = call.receive<SoknadFraHotsakNummerDto>()
            val soknadId = transaction {
                hotsakStore.hentSøknadsIdForHotsakNummer(soknadFraHotsakNummerDto.saksnummer)
            }
            logg.info("Fant søknadsid $soknadId fra HOTSAK nummer ${soknadFraHotsakNummerDto.saksnummer}")

            soknadId.let { call.respond(mapOf("soknadId" to soknadId)) }
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved henting av søknad fra HOTSAK data" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad fra HOTSAK data")
        }
    }

    post("/soknad/hotsak/har-vedtak/fra-søknadid") {
        try {
            val soknadId = call.receive<HarVedtakFraHotsakSøknadIdDto>().søknadId
            val harVedtak = transaction { hotsakStore.harVedtakForSøknadId(soknadId) }
            logg.info("Fant harVedtak $harVedtak fra HOTSAK med søknadId $soknadId")

            soknadId.let { call.respond(mapOf("harVedtak" to harVedtak)) }
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved henting av harVedtak fra HOTSAK data" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av harVedtak fra HOTSAK data")
        }
    }

    post("/hotsak/vedtaksresultat") {
        try {
            val vedtaksresultatToBeSaved = call.receive<VedtaksresultatDto>()
            val rowUpdated = transaction {
                hotsakStore.lagreVedtaksresultat(
                    vedtaksresultatToBeSaved.søknadId,
                    vedtaksresultatToBeSaved.vedtaksresultat,
                    vedtaksresultatToBeSaved.vedtaksdato,
                )
            }
            call.respond(rowUpdated)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved lagring av vedtaksresultat fra hotsak" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av vedtaksresultat fra hotsak")
        }
    }

    delete("/soknad/bruker") {
        try {
            val soknadToBeDeleted = call.receive<UUID>()
            val rowsDeleted = transaction { søknadStore.slettSøknad(soknadToBeDeleted) }
            call.respond(rowsDeleted)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved sletting av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved sletting av søknad")
        }
    }

    delete("/soknad/utlopt/bruker") {
        try {
            val soknadToBeDeleted = call.receive<UUID>()
            val rowsDeleted = transaction { søknadStore.slettUtløptSøknad(soknadToBeDeleted) }
            call.respond(rowsDeleted)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved sletting av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved sletting av søknad")
        }
    }

    put("/soknad/status/{soknadId}") {
        try {
            val søknadId = call.søknadId
            val newStatus = call.receive<Status>()
            val rowsUpdated = transaction { søknadStore.oppdaterStatus(søknadId, newStatus) }
            call.respond(rowsUpdated)

            metrics.measureElapsedTimeBetweenStatusChanges(søknadId, newStatus)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved oppdatering av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved oppdatering av søknad")
        }
    }

    put("/soknad/statusV2") {
        try {
            val statusMedÅrsak = call.receive<StatusMedÅrsak>()
            val rowsUpdated = transaction { søknadStore.oppdaterStatusMedÅrsak(statusMedÅrsak) }
            call.respond(rowsUpdated)

            metrics.measureElapsedTimeBetweenStatusChanges(statusMedÅrsak.søknadId, statusMedÅrsak.status)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved oppdatering av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved oppdatering av søknad")
        }
    }

    get("/soknad/bruker/finnes/{soknadId}") {
        try {
            val søknadId = call.søknadId
            val søknadFinnes = transaction { søknadStore.søknadFinnes(søknadId) }
            call.respond("soknadFinnes" to søknadFinnes)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved henting av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad")
        }
    }

    post("/infotrygd/fnr-jounralpost") {
        try {
            val fnrOgJournalpostIdFinnesDto = call.receive<FnrOgJournalpostIdFinnesDto>()
            val fnrOgJournalpostIdFinnes = transaction {
                søknadStore.fnrOgJournalpostIdFinnes(
                    fnrOgJournalpostIdFinnesDto.fnrBruker,
                    fnrOgJournalpostIdFinnesDto.journalpostId,
                )
            }

            when {
                fnrOgJournalpostIdFinnes -> {
                    call.respond("fnrOgJournalpostIdFinnes" to true)
                }

                else -> {
                    call.respond("fnrOgJournalpostIdFinnes" to false)
                }
            }
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved henting av fnr og journalpost" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av fnr og journalpost")
        }
    }

    get("/soknadsdata/bruker/{soknadId}") {
        try {
            val søknadId = call.søknadId
            val søknad = transaction { søknadStore.hentSoknadData(søknadId) }

            when (søknad) {
                null -> {
                    call.respond(HttpStatusCode.NotFound)
                }

                else -> {
                    call.respond(søknad)
                }
            }
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved henting av søknadsdata" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknadsdata")
        }
    }

    post("/soknad/fra-vedtaksresultat") {
        try {
            val soknadFraVedtaksresultatDto = call.receive<SoknadFraVedtaksresultatDto>()
            val soknadId = transaction {
                infotrygdStore.hentSøknadIdFraVedtaksresultat(
                    soknadFraVedtaksresultatDto.fnrBruker,
                    soknadFraVedtaksresultatDto.saksblokkOgSaksnr,
                    soknadFraVedtaksresultatDto.vedtaksdato,
                )
            }
            call.respond(mapOf(Pair("soknadId", soknadId)))
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved henting av søknad fra vedtaksdata" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad fra vedtaksdata")
        }
    }

    post("/soknad/fra-vedtaksresultat-v2") {
        try {
            val soknadFraVedtaksresultatDto = call.receive<SoknadFraVedtaksresultatV2Dto>()
            val resultater = transaction {
                infotrygdStore.hentSøknadIdFraVedtaksresultatV2(
                    soknadFraVedtaksresultatDto.fnrBruker,
                    soknadFraVedtaksresultatDto.saksblokkOgSaksnr,
                )
            }
            call.respond(resultater)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved henting av søknad fra vedtaksdata" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad fra vedtaksdata")
        }
    }

    get("/soknad/opprettet-dato/{soknadId}") {
        try {
            val søknadId = call.søknadId
            val opprettetDato = transaction { søknadStore.hentSoknadOpprettetDato(søknadId) }

            when (opprettetDato) {
                null -> {
                    call.respond(HttpStatusCode.NotFound)
                }

                else -> {
                    call.respond(opprettetDato)
                }
            }
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved henting av opprettet dato" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av opprettet dato")
        }
    }

    get("/soknad/utgaatt/{dager}") {
        val dager = call.parameters["dager"]?.toInt() ?: throw BadRequestException("Parameter 'dager' var ugyldig")

        try {
            val soknaderTilGodkjenningEldreEnn = transaction { søknadStore.hentSoknaderTilGodkjenningEldreEnn(dager) }
            call.respond(soknaderTilGodkjenningEldreEnn)
        } catch (e: Exception) {
            logg.error(e) { "Error on fetching søknader til godkjenning eldre enn" }
            call.respond(HttpStatusCode.InternalServerError, e)
        }
    }

    put("/soknad/journalpost-id/{soknadId}") {
        try {
            val søknadId = call.søknadId
            val newJournalpostDto = call.receive<Map<String, String>>()
            val journalpostId = newJournalpostDto["journalpostId"] ?: throw Exception("journalpostId mangler i body")
            val rowsUpdated = transaction { søknadStore.oppdaterJournalpostId(søknadId, journalpostId) }
            call.respond(rowsUpdated)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved oppdatering av journalpost-id" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved oppdatering av journalpost-id")
        }
    }

    put("/soknad/oppgave-id/{soknadId}") {
        try {
            val søknadId = call.søknadId
            val newOppgaveDto = call.receive<Map<String, String>>()
            val oppgaveId = newOppgaveDto["oppgaveId"] ?: throw Exception("No oppgaveId in body")
            val rowsUpdated = transaction { søknadStore.oppdaterOppgaveId(søknadId, oppgaveId) }
            call.respond(rowsUpdated)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved oppdatering av oppgave-id" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved oppdatering av oppgave-id")
        }
    }

    get("/soknad/ordre/ordrelinje-siste-doegn/{soknadId}") {
        try {
            val soknadsId = call.søknadId
            val result = transaction { ordreStore.ordreSisteDøgn(soknadsId) }
            call.respond(result)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved sjekk om en ordre har blitt oppdatert det siste døgnet" }
            call.respond(
                HttpStatusCode.BadRequest,
                "Feilet ved sjekk om en ordre har blitt oppdatert det siste døgnet",
            )
        }
    }

    get("/soknad/ordre/har-ordre/{soknadId}") {
        try {
            val søknadId = call.søknadId
            val result = transaction { ordreStore.harOrdre(søknadId) }
            call.respond(result)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved sjekk om en søknad har ordre" }
            call.respond(
                HttpStatusCode.BadRequest,
                "Feilet ved sjekk om en søknad har ordre",
            )
        }
    }

    get("/soknad/behovsmeldingType/{soknadId}") {
        try {
            val søknadId = call.søknadId
            val result = transaction { søknadStore.behovsmeldingTypeFor(søknadId) }
            if (result == null) {
                logg.info("Failed to get result for behovsmeldingType (result=$result) for søknadsId=$søknadId")
            } else {
                logg.info("Found behovsmeldingType=$result for soknadsId=$søknadId")
            }
            data class Result(val behovsmeldingType: BehovsmeldingType?)
            call.respond(Result(result))
        } catch (e: Exception) {
            logg.error(e) { "Kunne ikke hente ut behovsmeldingsType" }
            call.respond(
                HttpStatusCode.BadRequest,
                "Kunne ikke hente ut behovsmeldingsType",
            )
        }
    }

    post("/kommune-api/soknader") {
        data class Request(
            val kommunenummer: String,
            val nyereEnn: UUID?,
            val nyereEnnTidsstempel: Long?,
        ) {
            fun isValid() =
                kommunenummer.isNotEmpty() &&
                    (kommunenummer.toIntOrNull()?.let { it in 0..10000 } ?: false)
        }

        val req = runCatching {
            val req = call.receive<Request>()
            if (!req.isValid()) throw IllegalArgumentException("Request not valid: $req")
            req
        }.getOrElse { e ->
            logg.error(e) { "Feilet ved henting av søknader for kommune-apiet" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknader for kommune-apiet")
            return@post
        }

        runCatching {
            val soknader = transaction {
                søknadStore.hentSoknaderForKommuneApiet(req.kommunenummer, req.nyereEnn, req.nyereEnnTidsstempel)
            }
            call.respond(soknader)
        }.getOrElse { e ->
            logg.error(e) { "Feilet ved henting av søknader for kommune-apiet" }
            call.respond(HttpStatusCode.InternalServerError, "Feilet ved henting av søknader for kommune-apiet")
            return@post
        }
    }

    get("/forslagsmotor/tilbehoer/datasett") {
        try {
            val result = transaction {
                søknadStore.initieltDatasettForForslagsmotorTilbehoer()
            }
            call.respond(result)
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved uthenting av initielt datasett for forslagsmotor for tilbehør" }
            call.respond(
                HttpStatusCode.InternalServerError,
                "Feilet ved uthenting av initielt datasett for forslagsmotor for tilbehør",
            )
        }
    }
}
