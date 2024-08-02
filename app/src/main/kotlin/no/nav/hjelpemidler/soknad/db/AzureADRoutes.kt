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
import no.nav.hjelpemidler.behovsmeldingsmodell.ordre.Ordrelinje
import no.nav.hjelpemidler.soknad.db.domain.PapirSøknadData
import no.nav.hjelpemidler.soknad.db.domain.SøknadData
import no.nav.hjelpemidler.soknad.db.ktor.søknadId
import no.nav.hjelpemidler.soknad.db.sak.sakApi
import no.nav.hjelpemidler.soknad.db.soknad.søknadApi
import no.nav.hjelpemidler.soknad.db.store.Transaction
import java.time.LocalDate
import java.util.UUID

private val logg = KotlinLogging.logger {}

fun Route.azureADRoutes(
    transaction: Transaction,
    serviceContext: ServiceContext,
) {
    søknadApi(transaction, serviceContext)
    sakApi(transaction)
    kommuneApi(transaction)

    // fixme -> vurder felles endepunkt for lagring av behovsmeldinger av alle slag
    post("/soknad/bruker") {
        val søknad = call.receive<SøknadData>()
        logg.info { "Digital behovsmelding mottatt for lagring, søknadId: ${søknad.soknadId}" }
        val rowsUpdated = transaction { søknadStore.lagreBehovsmelding(søknad) }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    // fixme -> vurder felles endepunkt for lagring av behovsmeldinger av alle slag
    post("/soknad/papir") {
        val søknad = call.receive<PapirSøknadData>()
        logg.info { "Papirsøknad mottatt for lagring, søknadId: ${søknad.søknadId}" }
        val rowsUpdated = transaction { søknadStore.lagrePapirsøknad(søknad) }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    // fixme -> slettes, bytt til POST /soknad/{soknadId}/ordre
    post("/ordre") {
        val ordrelinje = call.receive<Ordrelinje>()
        logg.info { "Ordrelinje mottatt for lagring, søknadId: ${ordrelinje.søknadId}" }
        val rowsUpdated = transaction { ordreStore.lagre(ordrelinje.søknadId, ordrelinje) }
        call.respond(HttpStatusCode.Created, rowsUpdated)
    }

    // fixme -> slettes, bytt til DELETE /soknad/{soknadId}
    delete("/soknad/bruker") {
        val søknadId = call.receive<UUID>()
        logg.info { "Sletter søknad med søknadId: $søknadId" }
        val rowsDeleted = transaction { søknadStore.slettSøknad(søknadId) }
        call.respond(rowsDeleted)
    }

    // fixme -> slettes, bytt til DELETE /soknad/{soknadId}?status=UTLØPT
    delete("/soknad/utlopt/bruker") {
        val søknadId = call.receive<UUID>()
        logg.info { "Sletter utløpt søknad med søknadId: $søknadId" }
        val rowsDeleted = transaction { søknadStore.slettUtløptSøknad(søknadId) }
        call.respond(rowsDeleted)
    }

    // fixme -> slettes, burde kunne orkestreres i backend
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

    // fixme -> finn et bedre pattern
    post("/soknad/fra-vedtaksresultat-v2") {
        data class Request(
            val fnrBruker: String,
            val saksblokkOgSaksnr: String,
        )

        data class Response(
            val søknadId: UUID,
            val vedtaksDato: LocalDate?,
        )

        val request = call.receive<Request>()
        val resultater = transaction {
            infotrygdStore.hentSøknadIdFraVedtaksresultatV2(
                request.fnrBruker,
                request.saksblokkOgSaksnr,
            )
        }.map { Response(it.søknadId, it.vedtak?.vedtaksdato) }

        call.respond(resultater)
    }

    get("/soknad/utgaatt/{dager}") {
        val dager = call.parameters["dager"]?.toInt() ?: throw BadRequestException("Parameter 'dager' var ugyldig")
        val søknader = transaction { søknadStore.hentSøknaderTilGodkjenningEldreEnn(dager) }
        call.respond(søknader)
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

    get("/forslagsmotor/tilbehoer/datasett") {
        logg.info { "Henter initielt datasett til forslagsmotoren for tilbehør" }
        val result = transaction {
            søknadStore.hentInitieltDatasettForForslagsmotorTilbehør()
        }
        call.respond(result)
    }
}
