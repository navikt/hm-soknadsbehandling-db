package no.nav.hjelpemidler.soknad.db

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
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
        val søknader = transaction {
            søknadStore.hentBehovsmeldingerTilGodkjenningEldreEnn(dager)
                .map { it.tilBehovsmeldingSomVenterGodkjenningDto() }
        }
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

    // Brukes til å tagge digital/papir i brevstatistikk fra Infotrygd!
    post("/infotrygd/digitale-vedtak-nokler") {
        data class Request(
            val fraOgMedDato: LocalDate,
            val tilOgMedDato: LocalDate,
        )
        val req = call.receive<Request>()
        val response = transaction { infotrygdStore.hentDigitaleVedtakNøkler(req.fraOgMedDato, req.tilOgMedDato) }
        call.respond(response)
    }
}
