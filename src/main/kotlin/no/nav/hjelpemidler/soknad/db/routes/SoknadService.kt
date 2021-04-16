package no.nav.hjelpemidler.soknad.db.routes

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.util.pipeline.PipelineContext
import mu.KotlinLogging
import no.nav.hjelpemidler.soknad.db.UserPrincipal
import no.nav.hjelpemidler.soknad.db.db.InfotrygdStore
import no.nav.hjelpemidler.soknad.db.db.OrdreStore
import no.nav.hjelpemidler.soknad.db.db.SøknadStore
import no.nav.hjelpemidler.soknad.db.db.SøknadStoreFormidler
import no.nav.hjelpemidler.soknad.db.domain.OrdrelinjeData
import no.nav.hjelpemidler.soknad.db.domain.PapirSøknadData
import no.nav.hjelpemidler.soknad.db.domain.SoknadData
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.domain.VedtaksresultatData
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal fun Route.hentSoknad(store: SøknadStore) {
    get("/soknad/bruker/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val fnr = call.principal<UserPrincipal>()?.getFnr()
                ?: call.respond(HttpStatusCode.BadRequest, "Fnr mangler i token claim")
            val soknad = store.hentSoknad(soknadsId)

            when {
                soknad == null -> {
                    call.respond(HttpStatusCode.NotFound)
                }
                soknad.fnrBruker != fnr -> {
                    call.respond(HttpStatusCode.Forbidden, "Søknad er ikke registrert på aktuell bruker")
                }
                else -> {
                    call.respond(soknad)
                }
            }
        } catch (e: Exception) {
            logger.error { "Feilet ved henting av søknad: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved henting av søknad ${e.message}")
        }
    }
}

internal fun Route.hentFnrForSoknad(store: SøknadStore) {
    get("/soknad/fnr/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val fnrForSoknad = store.hentFnrForSoknad(soknadsId)
            call.respond(fnrForSoknad)
        } catch (e: Exception) {
            logger.error { "Feilet ved henting av søknad: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved henting av søknad ${e.message}")
        }
    }
}

internal fun Route.saveSoknad(store: SøknadStore) {
    post("/soknad/bruker") {
        try {
            val soknadToBeSaved = call.receive<SoknadData>()
            store.save(soknadToBeSaved)
            call.respond("OK")
        } catch (e: Exception) {
            logger.error { "Feilet ved lagring av søknad: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved lagring av søknad ${e.message}")
        }
    }
}

internal fun Route.saveOrdrelinje(store: OrdreStore) {
    post("/ordre") {
        try {
            val ordreToBeSaved = call.receive<OrdrelinjeData>()
            store.save(ordreToBeSaved)
            call.respond("OK")
        } catch (e: Exception) {
            logger.error { "Feilet ved lagring av ordrelinje: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved lagring av ordrelinje ${e.message}")
        }
    }
}

internal fun Route.savePapir(store: SøknadStore) {
    post("/soknad/papir") {
        try {
            val papirsoknadToBeSaved = call.receive<PapirSøknadData>()
            val rowsUpdated = store.savePapir(papirsoknadToBeSaved)
            call.respond(rowsUpdated)
        } catch (e: Exception) {
            logger.error { "Feilet ved lagring av papirsøknad: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved lagring av papirsøknad ${e.message}")
        }
    }
}

internal fun Route.lagKnytningMellomFagsakOgSøknad(store: InfotrygdStore) {
    post("/infotrygd/fagsak") {
        try {
            val vedtaksresultatData = call.receive<VedtaksresultatData>()
            store.lagKnytningMellomFagsakOgSøknad(vedtaksresultatData)
            call.respond("OK")
        } catch (e: Exception) {
            logger.error { "Feilet ved lagring av ordrelinje: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved lagring av ordrelinje ${e.message}")
        }
    }
}

internal fun Route.lagreVedtaksresultat(store: InfotrygdStore) {
    post("/infotrygd/vedtaksresultat") {
        try {
            val vedtaksresultatToBeSaved = call.receive<VedtaksresultatDto>()
            val rowUpdated = store.lagreVedtaksresultat(vedtaksresultatToBeSaved.søknadId, vedtaksresultatToBeSaved.vedtaksresultat, vedtaksresultatToBeSaved.vedtaksdato)
            call.respond(rowUpdated)
        } catch (e: Exception) {
            logger.error { "Feilet ved lagring av vedtaksresultat: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved lagring av vedtaksresultat ${e.message}")
        }
    }
}

data class VedtaksresultatDto(
    val søknadId: UUID,
    val vedtaksresultat: String,
    val vedtaksdato: LocalDate
)

internal fun Route.slettSøknad(store: SøknadStore) {
    delete("/soknad/bruker") {
        try {
            val soknadToBeDeleted = call.receive<UUID>()
            val rowsDeleted = store.slettSøknad(soknadToBeDeleted)
            call.respond(rowsDeleted)
        } catch (e: Exception) {
            logger.error { "Feilet ved sletting av søknad: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved sletting av søknad ${e.message}")
        }
    }
}

internal fun Route.slettUtløptSøknad(store: SøknadStore) {
    delete("/soknad/utlopt/bruker") {
        try {
            val soknadToBeDeleted = call.receive<UUID>()
            val rowsDeleted = store.slettUtløptSøknad(soknadToBeDeleted)
            call.respond(rowsDeleted)
        } catch (e: Exception) {
            logger.error { "Feilet ved sletting av søknad: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved sletting av søknad ${e.message}")
        }
    }
}

internal fun Route.oppdaterStatus(store: SøknadStore) {
    put("/soknad/status/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val newStatus = call.receive<Status>()
            val rowsUpdated = store.oppdaterStatus(soknadsId, newStatus)
            call.respond(rowsUpdated)
        } catch (e: Exception) {
            logger.error { "Feilet ved oppdatering av søknad: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved oppdatering av søknad ${e.message}")
        }
    }
}

internal fun Route.hentSoknaderForBruker(store: SøknadStore) {
    get("/soknad/bruker") {

        val fnr = call.principal<UserPrincipal>()?.getFnr() ?: throw RuntimeException("Fnr mangler i token claim")

        try {
            val soknaderTilGodkjenning = store.hentSoknaderForBruker(fnr)
            call.respond(soknaderTilGodkjenning)
        } catch (e: Exception) {
            logger.error(e) { "Error on fetching søknader til godkjenning" }
            call.respond(HttpStatusCode.InternalServerError, e)
        }
    }
}

internal fun Route.hentSoknaderForFormidler(store: SøknadStoreFormidler) {
    get("/soknad/formidler") {

        val fnr = call.principal<UserPrincipal>()?.getFnr() ?: throw RuntimeException("Fnr mangler i token claim")

        try {
            val formidlersSøknader = store.hentSøknaderForFormidler(fnr, 4)
            call.respond(formidlersSøknader)
        } catch (e: Exception) {
            logger.error(e) { "Error on fetching formidlers søknader" }
            call.respond(HttpStatusCode.InternalServerError, e)
        }
    }
}

internal fun Route.soknadFinnes(store: SøknadStore) {
    get("/soknad/bruker/finnes/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val soknadFinnes = store.soknadFinnes(soknadsId)

            when {
                soknadFinnes -> {
                    call.respond("soknadFinnes" to true)
                }
                else -> {
                    call.respond("soknadFinnes" to false)
                }
            }
        } catch (e: Exception) {
            logger.error { "Feilet ved henting av søknad: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved henting av søknad ${e.message}")
        }
    }
}

internal fun Route.fnrOgJournalpostIdFinnes(store: SøknadStore) {
    post("/infotrygd/fnr-jounralpost") {
        try {

            val fnrOgJournalpostIdFinnesDto = call.receive<FnrOgJournalpostIdFinnesDto>()
            val fnrOgJournalpostIdFinnes = store.fnrOgJournalpostIdFinnes(fnrOgJournalpostIdFinnesDto.fnrBruker, fnrOgJournalpostIdFinnesDto.journalpostId)

            when {
                fnrOgJournalpostIdFinnes -> {
                    call.respond("fnrOgJournalpostIdFinnes" to true)
                }
                else -> {
                    call.respond("fnrOgJournalpostIdFinnes" to false)
                }
            }
        } catch (e: Exception) {
            logger.error { "Feilet ved henting av fnr og journalpost: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved henting av fnr og journalpost ${e.message}")
        }
    }
}

data class FnrOgJournalpostIdFinnesDto(
    val fnrBruker: String,
    val journalpostId: Int
)

internal fun Route.hentSoknadsdata(store: SøknadStore) {
    get("/soknadsdata/bruker/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val soknad = store.hentSoknadData(soknadsId)

            when (soknad) {
                null -> {
                    call.respond(HttpStatusCode.NotFound)
                }
                else -> {
                    call.respond(soknad)
                }
            }
        } catch (e: Exception) {
            logger.error { "Feilet ved henting av søknadsdata: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved henting av søknadsdata ${e.message}")
        }
    }
}

internal fun Route.hentSøknadIdFraVedtaksresultat(store: InfotrygdStore) {
    post("/soknad/fra-vedtaksresultat") {
        try {

            val soknadFraVedtaksresultatDto = call.receive<SoknadFraVedtaksresultatDto>()
            val soknadId = store.hentSøknadIdFraVedtaksresultat(soknadFraVedtaksresultatDto.fnrBruker, soknadFraVedtaksresultatDto.saksblokkOgSaksnr, soknadFraVedtaksresultatDto.vedtaksdato)
            when (soknadId) {
                null -> {
                    call.respond(HttpStatusCode.NotFound)
                }
                else -> {
                    call.respond(soknadId)
                }
            }
        } catch (e: Exception) {
            logger.error { "Feilet ved henting av søknad fra vedtaksdata: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved henting av søknad fra vedtaksdata ${e.message}")
        }
    }
}

data class SoknadFraVedtaksresultatDto(
    val fnrBruker: String,
    val saksblokkOgSaksnr: String,
    val vedtaksdato: LocalDate
)

internal fun Route.hentSoknadOpprettetDato(store: SøknadStore) {
    get("/soknad/opprettet-dato/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val opprettetDato = store.hentSoknadOpprettetDato(soknadsId)

            when (opprettetDato) {
                null -> {
                    call.respond(HttpStatusCode.NotFound)
                }
                else -> {
                    call.respond(opprettetDato)
                }
            }
        } catch (e: Exception) {
            logger.error { "Feilet ved henting av opprettet dato: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved henting av opprettet dato ${e.message}")
        }
    }
}

internal fun Route.hentSoknaderTilGodkjenningEldreEnn(store: SøknadStore) {
    get("/soknad/utgaatt/{dager}") {

        val dager = call.parameters["dager"]?.toInt() ?: throw RuntimeException("Parameter 'dager' var ugyldig")

        try {
            val soknaderTilGodkjenningEldreEnn = store.hentSoknaderTilGodkjenningEldreEnn(dager)
            call.respond(soknaderTilGodkjenningEldreEnn)
        } catch (e: Exception) {
            logger.error(e) { "Error on fetching søknader til godkjenning eldre enn" }
            call.respond(HttpStatusCode.InternalServerError, e)
        }
    }
}

internal fun Route.oppdaterJournalpostId(store: SøknadStore) {
    put("/soknad/journalpost-id/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val newJournalpostId = call.receive<String>()
            val rowsUpdated = store.oppdaterJournalpostId(soknadsId, newJournalpostId)
            call.respond(rowsUpdated)
        } catch (e: Exception) {
            logger.error { "Feilet ved oppdatering av journalpost-id: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved oppdatering av journalpost-id ${e.message}")
        }
    }
}

internal fun Route.oppdaterOppgaveId(store: SøknadStore) {
    put("/soknad/oppgave-id/{soknadsId}") {
        try {
            val soknadsId = UUID.fromString(soknadsId())
            val newOppgaveId = call.receive<String>()
            val rowsUpdated = store.oppdaterJournalpostId(soknadsId, newOppgaveId)
            call.respond(rowsUpdated)
        } catch (e: Exception) {
            logger.error { "Feilet ved oppdatering av oppgave-id: ${e.message}. ${e.stackTrace}" }
            call.respond(HttpStatusCode.BadRequest, "Feil ved oppdatering av oppgave-id ${e.message}")
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.soknadsId() =
    call.parameters["soknadsId"]
