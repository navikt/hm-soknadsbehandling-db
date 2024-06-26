package no.nav.hjelpemidler.soknad.db.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.hjelpemidler.soknad.db.db.HotsakStore
import no.nav.hjelpemidler.soknad.db.db.OrdreStore
import no.nav.hjelpemidler.soknad.db.db.SøknadStore
import no.nav.hjelpemidler.soknad.db.db.SøknadStoreInnsender
import no.nav.hjelpemidler.soknad.db.domain.BehovsmeldingType
import no.nav.hjelpemidler.soknad.db.domain.ForslagsmotorTilbehørHjelpemidler
import no.nav.hjelpemidler.soknad.db.domain.HotsakTilknytningData
import no.nav.hjelpemidler.soknad.db.domain.OrdrelinjeData
import no.nav.hjelpemidler.soknad.db.domain.PapirSøknadData
import no.nav.hjelpemidler.soknad.db.domain.SoknadData
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.domain.StatusMedÅrsak
import no.nav.hjelpemidler.soknad.db.domain.VedtaksresultatData
import no.nav.hjelpemidler.soknad.db.metrics.Metrics
import no.nav.hjelpemidler.soknad.db.rolle.RolleService
import no.nav.hjelpemidler.soknad.mottak.db.InfotrygdStore
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import java.security.MessageDigest
import java.time.LocalDate
import java.util.Date
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal fun Route.tokenXRoutes(
    søknadStore: SøknadStore,
    ordreStore: OrdreStore,
    infotrygdStore: InfotrygdStore,
    hotsakStore: HotsakStore,
    formidlerStore: SøknadStoreInnsender,
    rolleService: RolleService,
    tokenXUserFactory: TokenXUserFactory = TokenXUserFactory,
) {
    get("/soknad/bruker/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val fnr = tokenXUserFactory.createTokenXUser(call).ident
            val soknad = søknadStore.hentSoknad(soknadsId)

            when {
                soknad == null -> {
                    call.respond(HttpStatusCode.NotFound)
                }

                soknad.fnrBruker != fnr -> {
                    call.respond(HttpStatusCode.Forbidden, "Søknad er ikke registrert på aktuell bruker")
                }

                else -> {
                    // Fetch ordrelinjer belonging to søknad
                    soknad.ordrelinjer = ordreStore.ordreForSoknad(soknad.søknadId)

                    // Fetch fagsakid if it exists
                    val fagsakData = infotrygdStore.hentFagsakIdForSøknad(soknad.søknadId)
                    if (fagsakData != null) {
                        soknad.fagsakId = fagsakData.fagsakId
                    } else {
                        val fagsakData2 = hotsakStore.hentFagsakIdForSøknad(soknad.søknadId)
                        if (fagsakData2 != null) soknad.fagsakId = fagsakData2
                    }

                    // Fetch soknadType for søknad
                    soknad.søknadType = infotrygdStore.hentTypeForSøknad(soknad.søknadId)

                    call.respond(soknad)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved henting av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad")
        }
    }

    get("/soknad/bruker") {
        val fnr = tokenXUserFactory.createTokenXUser(call).ident

        try {
            val brukersSaker = søknadStore.hentSoknaderForBruker(fnr)
            call.respond(brukersSaker)
        } catch (e: Exception) {
            logger.error(e) { "Error on fetching søknader til godkjenning" }
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av saker")
        }
    }

    get("/soknad/innsender") {
        val user = tokenXUserFactory.createTokenXUser(call)
        val fnrInnsender = user.ident
        val innsenderRolle = rolleService.hentRolle(user.tokenString)

        try {
            val formidlersSøknader = formidlerStore.hentSøknaderForInnsender(fnrInnsender, innsenderRolle)

            // Logg tilfeller av gamle saker hos formidler for statistikk, anonymiser fnr med enveis-sha256
            val olderThan6mo = java.sql.Date.valueOf(LocalDate.now().minusMonths(6))
            val datoer = mutableListOf<Date>()
            formidlersSøknader.forEach {
                if (it.datoOpprettet.before(olderThan6mo)) {
                    datoer.add(it.datoOpprettet)
                }
            }
            if (datoer.isNotEmpty()) {
                val bytes = fnrInnsender.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hash = digest.fold("") { str, byt -> str + "%02x".format(byt) }.take(10)
                val lastTen = datoer.takeLast(10).reversed().joinToString { it.toString() }
                logger.info("Formidlersiden ble lastet inn med sak(er) eldre enn 6mnd.: id=$hash, tilfeller=${datoer.count()} stk., datoOpprettet(siste 10): $lastTen.")
            }

            call.respond(formidlersSøknader)
        } catch (e: Exception) {
            logger.error(e) { "Error on fetching formidlers søknader" }
            call.respond(HttpStatusCode.InternalServerError, e)
        }
    }

    get("/soknad/innsender/{soknadsId}") {
        val soknadsId = UUID.fromString(soknadsId())
        val user = tokenXUserFactory.createTokenXUser(call)
        val fnrInnsender = user.ident
        val innsenderRolle = rolleService.hentRolle(user.tokenString)

        try {
            val formidlersSoknad = formidlerStore.hentSøknadForInnsender(fnrInnsender, soknadsId, innsenderRolle)
            if (formidlersSoknad == null) {
                logger.warn { "En formidler forsøkte å hente søknad <$soknadsId>, men den er ikke tilgjengelig for formidler nå" }
                call.respond(status = HttpStatusCode.NotFound, "Søknaden er ikke tilgjengelig for innlogget formidler")
            } else {
                logger.info { "Formidler hentet ut søknad $soknadsId" }
                call.respond(formidlersSoknad)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error on fetching formidlers søknader" }
            call.respond(HttpStatusCode.InternalServerError, e)
        }
    }

    get("/validerSøknadsidOgStatusVenterGodkjenning/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val fnr = tokenXUserFactory.createTokenXUser(call).ident
            val soknad = søknadStore.hentSoknad(soknadsId)

            when {
                soknad == null -> {
                    call.respond(ValiderSøknadsidOgStatusVenterGodkjenningRespons(false))
                }

                soknad.fnrBruker != fnr -> {
                    call.respond(ValiderSøknadsidOgStatusVenterGodkjenningRespons(false))
                }

                else -> {
                    call.respond(ValiderSøknadsidOgStatusVenterGodkjenningRespons(soknad.status == Status.VENTER_GODKJENNING))
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved henting av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad")
        }
    }
}

internal fun Route.azureAdRoutes(
    søknadStore: SøknadStore,
    ordreStore: OrdreStore,
    infotrygdStore: InfotrygdStore,
    hotsakStore: HotsakStore,
    metrics: Metrics,
) {
    get("/soknad/fnr/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val fnrForSoknad = søknadStore.hentFnrForSoknad(soknadsId)
            call.respond(fnrForSoknad)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved henting av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad")
        }
    }

    post("/soknad/bruker") {
        try {
            val soknadToBeSaved = call.receive<SoknadData>()
            søknadStore.save(soknadToBeSaved)
            call.respond("OK")
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved lagring av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av søknad")
        }
    }

    post("/ordre") {
        try {
            val ordreToBeSaved = call.receive<OrdrelinjeData>()
            val rowsUpdated = ordreStore.save(ordreToBeSaved)
            call.respond(rowsUpdated)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved lagring av ordrelinje" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av ordrelinje")
        }
    }

    post("/soknad/papir") {
        try {
            val papirsoknadToBeSaved = call.receive<PapirSøknadData>()
            val rowsUpdated = søknadStore.savePapir(papirsoknadToBeSaved)
            call.respond(rowsUpdated)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved lagring av papirsøknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av papirsøknad")
        }
    }

    post("/infotrygd/fagsak") {
        try {
            val vedtaksresultatData = call.receive<VedtaksresultatData>()
            val numRows = infotrygdStore.lagKnytningMellomFagsakOgSøknad(vedtaksresultatData)
            call.respond(numRows)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved lagring av ordrelinje" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av ordrelinje")
        }
    }

    post("/hotsak/sak") {
        try {
            val hotsakTilknytningData = call.receive<HotsakTilknytningData>()
            val numRows = hotsakStore.lagKnytningMellomSakOgSøknad(hotsakTilknytningData)
            call.respond(numRows)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved lagring av hotsak-tilknytning" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av hotsak-tilknytning")
        }
    }

    post("/infotrygd/vedtaksresultat") {
        try {
            val vedtaksresultatToBeSaved = call.receive<VedtaksresultatDto>()
            val rowUpdated = infotrygdStore.lagreVedtaksresultat(
                vedtaksresultatToBeSaved.søknadId,
                vedtaksresultatToBeSaved.vedtaksresultat,
                vedtaksresultatToBeSaved.vedtaksdato,
                vedtaksresultatToBeSaved.soknadsType,
            )
            call.respond(rowUpdated)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved lagring av vedtaksresultat" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av vedtaksresultat")
        }
    }

    get("/infotrygd/søknadsType/{soknadsId}") {
        val soknadsId = UUID.fromString(soknadsId())
        val søknadsType = infotrygdStore.hentTypeForSøknad(soknadsId)

        data class Response(val søknadsType: String?)
        call.respond(Response(søknadsType))
    }

    post("/soknad/hotsak/fra-saknummer") {
        try {
            val soknadFraHotsakNummerDto = call.receive<SoknadFraHotsakNummerDto>()
            val soknadId = hotsakStore.hentSøknadsIdForHotsakNummer(
                soknadFraHotsakNummerDto.saksnummer,
            )
            logger.info("Fant søknadsid $soknadId fra HOTSAK nummer ${soknadFraHotsakNummerDto.saksnummer}")

            soknadId.let { call.respond(mapOf("soknadId" to soknadId)) }
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved henting av søknad fra HOTSAK data" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad fra HOTSAK data")
        }
    }

    post("/soknad/hotsak/har-vedtak/fra-søknadid") {
        try {
            val soknadId = call.receive<HarVedtakFraHotsakSøknadIdDto>().søknadId
            val harVedtak = hotsakStore.harVedtakForSøknadId(
                soknadId,
            )
            logger.info("Fant harVedtak $harVedtak fra HOTSAK med søknadId $soknadId")

            soknadId.let { call.respond(mapOf("harVedtak" to harVedtak)) }
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved henting av harVedtak fra HOTSAK data" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av harVedtak fra HOTSAK data")
        }
    }

    post("/hotsak/vedtaksresultat") {
        try {
            val vedtaksresultatToBeSaved = call.receive<VedtaksresultatDto>()
            val rowUpdated = hotsakStore.lagreVedtaksresultat(
                vedtaksresultatToBeSaved.søknadId,
                vedtaksresultatToBeSaved.vedtaksresultat,
                vedtaksresultatToBeSaved.vedtaksdato,
            )
            call.respond(rowUpdated)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved lagring av vedtaksresultat fra hotsak" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved lagring av vedtaksresultat fra hotsak")
        }
    }

    delete("/soknad/bruker") {
        try {
            val soknadToBeDeleted = call.receive<UUID>()
            val rowsDeleted = søknadStore.slettSøknad(soknadToBeDeleted)
            call.respond(rowsDeleted)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved sletting av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved sletting av søknad")
        }
    }

    delete("/soknad/utlopt/bruker") {
        try {
            val soknadToBeDeleted = call.receive<UUID>()
            val rowsDeleted = søknadStore.slettUtløptSøknad(soknadToBeDeleted)
            call.respond(rowsDeleted)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved sletting av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved sletting av søknad")
        }
    }

    put("/soknad/status/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val newStatus = call.receive<Status>()
            val rowsUpdated = søknadStore.oppdaterStatus(soknadsId, newStatus)
            call.respond(rowsUpdated)

            metrics.measureElapsedTimeBetweenStatusChanges(soknadsId, newStatus)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved oppdatering av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved oppdatering av søknad")
        }
    }

    put("/soknad/statusV2") {
        try {
            val statusMedÅrsak = call.receive<StatusMedÅrsak>()
            val rowsUpdated = søknadStore.oppdaterStatusMedÅrsak(statusMedÅrsak)
            call.respond(rowsUpdated)

            metrics.measureElapsedTimeBetweenStatusChanges(statusMedÅrsak.søknadId, statusMedÅrsak.status)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved oppdatering av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved oppdatering av søknad")
        }
    }

    get("/soknad/bruker/finnes/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val soknadFinnes = søknadStore.soknadFinnes(soknadsId)

            when {
                soknadFinnes -> {
                    call.respond("soknadFinnes" to true)
                }

                else -> {
                    call.respond("soknadFinnes" to false)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved henting av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad")
        }
    }

    post("/infotrygd/fnr-jounralpost") {
        try {
            val fnrOgJournalpostIdFinnesDto = call.receive<FnrOgJournalpostIdFinnesDto>()
            val fnrOgJournalpostIdFinnes = søknadStore.fnrOgJournalpostIdFinnes(
                fnrOgJournalpostIdFinnesDto.fnrBruker,
                fnrOgJournalpostIdFinnesDto.journalpostId,
            )

            when {
                fnrOgJournalpostIdFinnes -> {
                    call.respond("fnrOgJournalpostIdFinnes" to true)
                }

                else -> {
                    call.respond("fnrOgJournalpostIdFinnes" to false)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved henting av fnr og journalpost" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av fnr og journalpost")
        }
    }

    get("/soknadsdata/bruker/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val soknad = søknadStore.hentSoknadData(soknadsId)

            when (soknad) {
                null -> {
                    call.respond(HttpStatusCode.NotFound)
                }

                else -> {
                    call.respond(soknad)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved henting av søknadsdata" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknadsdata")
        }
    }

    post("/soknad/fra-vedtaksresultat") {
        try {
            val soknadFraVedtaksresultatDto = call.receive<SoknadFraVedtaksresultatDto>()
            val soknadId = infotrygdStore.hentSøknadIdFraVedtaksresultat(
                soknadFraVedtaksresultatDto.fnrBruker,
                soknadFraVedtaksresultatDto.saksblokkOgSaksnr,
                soknadFraVedtaksresultatDto.vedtaksdato,
            )

            call.respond(mapOf(Pair("soknadId", soknadId)))
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved henting av søknad fra vedtaksdata" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad fra vedtaksdata")
        }
    }

    post("/soknad/fra-vedtaksresultat-v2") {
        try {
            val soknadFraVedtaksresultatDto = call.receive<SoknadFraVedtaksresultatV2Dto>()
            val resultater = infotrygdStore.hentSøknadIdFraVedtaksresultatV2(
                soknadFraVedtaksresultatDto.fnrBruker,
                soknadFraVedtaksresultatDto.saksblokkOgSaksnr,
            )

            call.respond(resultater)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved henting av søknad fra vedtaksdata" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad fra vedtaksdata")
        }
    }

    get("/soknad/opprettet-dato/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val opprettetDato = søknadStore.hentSoknadOpprettetDato(soknadsId)

            when (opprettetDato) {
                null -> {
                    call.respond(HttpStatusCode.NotFound)
                }

                else -> {
                    call.respond(opprettetDato)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved henting av opprettet dato" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av opprettet dato")
        }
    }

    get("/soknad/utgaatt/{dager}") {
        val dager = call.parameters["dager"]?.toInt() ?: throw RuntimeException("Parameter 'dager' var ugyldig")

        try {
            val soknaderTilGodkjenningEldreEnn = søknadStore.hentSoknaderTilGodkjenningEldreEnn(dager)
            call.respond(soknaderTilGodkjenningEldreEnn)
        } catch (e: Exception) {
            logger.error(e) { "Error on fetching søknader til godkjenning eldre enn" }
            call.respond(HttpStatusCode.InternalServerError, e)
        }
    }

    put("/soknad/journalpost-id/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val newJournalpostDto = call.receive<Map<String, String>>()
            val journalpostId = newJournalpostDto["journalpostId"] ?: throw Exception("journalpostId mangler i body")
            val rowsUpdated = søknadStore.oppdaterJournalpostId(soknadsId, journalpostId)
            call.respond(rowsUpdated)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved oppdatering av journalpost-id" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved oppdatering av journalpost-id")
        }
    }

    put("/soknad/oppgave-id/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val newOppgaveDto = call.receive<Map<String, String>>()
            val oppgaveId = newOppgaveDto["oppgaveId"] ?: throw Exception("No oppgaveId in body")
            val rowsUpdated = søknadStore.oppdaterOppgaveId(soknadsId, oppgaveId)
            call.respond(rowsUpdated)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved oppdatering av oppgave-id" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved oppdatering av oppgave-id")
        }
    }

    get("/soknad/ordre/ordrelinje-siste-doegn/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val result = ordreStore.ordreSisteDøgn(soknadsId)
            call.respond(result)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved sjekk om en ordre har blitt oppdatert det siste døgnet" }
            call.respond(
                HttpStatusCode.BadRequest,
                "Feilet ved sjekk om en ordre har blitt oppdatert det siste døgnet",
            )
        }
    }

    get("/soknad/ordre/har-ordre/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val result = ordreStore.harOrdre(soknadsId)
            call.respond(result)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved sjekk om en søknad har ordre" }
            call.respond(
                HttpStatusCode.BadRequest,
                "Feilet ved sjekk om en søknad har ordre",
            )
        }
    }

    get("/soknad/behovsmeldingType/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val result = søknadStore.behovsmeldingTypeFor(soknadsId)
            if (result == null) {
                logger.info("Failed to get result for behovsmeldingType (result=$result) for søknadsId=$soknadsId")
            } else {
                logger.info("Found behovsmeldingType=$result for soknadsId=$soknadsId")
            }
            data class Result(val behovsmeldingType: BehovsmeldingType?)
            call.respond(Result(result))
        } catch (e: Exception) {
            logger.error(e) { "Kunne ikke hente ut behovsmeldingsType" }
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
            logger.error(e) { "Feilet ved henting av søknader for kommune-apiet" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknader for kommune-apiet")
            return@post
        }

        runCatching {
            val soknader =
                søknadStore.hentSoknaderForKommuneApiet(req.kommunenummer, req.nyereEnn, req.nyereEnnTidsstempel)
            call.respond(soknader)
        }.getOrElse { e ->
            logger.error(e) { "Feilet ved henting av søknader for kommune-apiet" }
            call.respond(HttpStatusCode.InternalServerError, "Feilet ved henting av søknader for kommune-apiet")
            return@post
        }
    }

    get("/forslagsmotor/tilbehoer/datasett") {
        try {
            var result: List<ForslagsmotorTilbehørHjelpemidler>?

            // The following withContext moves execution off onto another thread so that ktor can continue answering
            // other clients (eg. kubernetes liveness tests). Without this the app would be assumed dead and killed by
            // Kubernetes.
            withContext(Dispatchers.IO) {
                result = søknadStore.initieltDatasettForForslagsmotorTilbehoer()
            }

            val finalResult = result ?: listOf()
            call.respond(finalResult)
        } catch (e: Exception) {
            logger.error(e) { "Feilet ved uthenting av initielt datasett for forslagsmotor for tilbehør" }
            call.respond(
                HttpStatusCode.InternalServerError,
                "Feilet ved uthenting av initielt datasett for forslagsmotor for tilbehør",
            )
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.soknadsId() =
    call.parameters["soknadsId"]

data class ValiderSøknadsidOgStatusVenterGodkjenningRespons(
    val resultat: Boolean,
)

data class VedtaksresultatDto(
    val søknadId: UUID,
    val vedtaksresultat: String,
    val vedtaksdato: LocalDate,
    val soknadsType: String,
)

data class FnrOgJournalpostIdFinnesDto(
    val fnrBruker: String,
    val journalpostId: Int,
)

data class SoknadFraVedtaksresultatDto(
    val fnrBruker: String,
    val saksblokkOgSaksnr: String,
    val vedtaksdato: LocalDate,
)

data class SoknadFraVedtaksresultatV2Dto(
    val fnrBruker: String,
    val saksblokkOgSaksnr: String,
)

data class SoknadFraHotsakNummerDto(val saksnummer: String)

data class HarVedtakFraHotsakSøknadIdDto(val søknadId: UUID)
