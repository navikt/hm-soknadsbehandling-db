package no.nav.hjelpemidler.soknad.db

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.application
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSak
import no.nav.hjelpemidler.kafka.KafkaMessage
import no.nav.hjelpemidler.server.ktor.respondUpstream
import no.nav.hjelpemidler.server.ktor.upstreamHeaders
import no.nav.hjelpemidler.soknad.db.bruker.Bruker
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.SafselvbetjeningClient
import no.nav.hjelpemidler.soknad.db.dokument.JournalpostJson
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.kafka.KafkaClient
import no.nav.hjelpemidler.soknad.db.ktor.Response
import no.nav.hjelpemidler.soknad.db.ktor.autentisertInnbygger
import no.nav.hjelpemidler.soknad.db.ordre.OrdreService
import no.nav.hjelpemidler.soknad.db.rolle.RolleService
import no.nav.hjelpemidler.soknad.db.soknad.Behovsmelding
import no.nav.hjelpemidler.soknad.db.soknad.StatusResponse
import no.nav.hjelpemidler.soknad.db.soknad.SøknadService
import no.nav.hjelpemidler.soknad.db.soknad.Søknader
import no.nav.hjelpemidler.soknad.db.store.Transaction
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.UUID

private val logg = KotlinLogging.logger {}

fun Route.tokenXRoutes() {
    val transaction: Transaction by application.dependencies

    val kafkaClient: KafkaClient by application.dependencies

    val ordreService: OrdreService by application.dependencies
    val rolleService: RolleService by application.dependencies
    val søknadService: SøknadService by application.dependencies

    val tokenXUserFactory: TokenXUserFactory by application.dependencies

    val safselvbetjeningClient: SafselvbetjeningClient by application.dependencies

    get<Søknader.Bruker.SøknadId> {
        val fnr = call.autentisertInnbygger().fnr
        val søknad = transaction { søknadStore.hentSøknad(it.søknadId) }

        when {
            søknad == null -> {
                call.feilmelding(HttpStatusCode.NotFound)
            }

            søknad.fnrBruker != fnr -> {
                call.feilmelding(HttpStatusCode.Forbidden, "Søknad er ikke registrert på aktuell bruker")
            }

            else -> {
                // Fetch ordrelinjer belonging to søknad
                søknad.ordrelinjer = ordreService.finnOrdreForSøknad(søknad.søknadId)

                val sak = søknadService.finnSak(søknad.søknadId)
                if (sak != null) {
                    søknad.fagsakId = sak.sakId.toString()
                }
                if (sak is InfotrygdSak) {
                    søknad.søknadType = sak.søknadstype
                }

                call.respond(søknad)
            }
        }
    }

    get<Søknader.Bruker> {
        val innbygger = call.autentisertInnbygger()
        val fnrBruker = innbygger.fnr
        val brukersSaker = transaction { søknadStore.hentSøknaderForBruker(fnrBruker) }
        call.respond(brukersSaker)
    }

    get<Søknader.Innsender> {
        val innbygger = call.autentisertInnbygger()
        val fnrInnsender = innbygger.fnr
        val innsenderRolle = rolleService.hentRolle(innbygger.userToken)

        val søknader = transaction {
            søknadStoreInnsender.hentSøknaderForInnsender(fnrInnsender, innsenderRolle)
        }

        // Logg tilfeller av gamle saker hos formidler for statistikk, anonymiser fnr med enveis-sha256
        val seksMånederSiden = LocalDateTime.now().minusMonths(6)
        val datoer = søknader
            .filter { it.datoOpprettet.isBefore(seksMånederSiden) }
            .map { it.datoOpprettet }
        if (datoer.isNotEmpty()) {
            val digest = MessageDigest.getInstance("SHA-256").digest(fnrInnsender.toByteArray())
            val hash = digest.fold("") { str, byt -> str + "%02x".format(byt) }.take(10)
            val sisteTi = datoer.takeLast(10).reversed().joinToString { it.toString() }
            logg.info { "Formidlersiden ble lastet inn med sak(er) eldre enn 6mnd., id: $hash, tilfeller: ${datoer.count()} stk., datoOpprettet(siste 10): $sisteTi." }
        }

        call.respond(søknader)
    }

    get<Søknader.Innsender.SøknadId> {
        val søknadId = it.søknadId
        val innbygger = call.autentisertInnbygger()
        val fnrInnsender = innbygger.fnr
        val innsenderRolle = rolleService.hentRolle(innbygger.userToken)

        val søknad = transaction {
            søknadStoreInnsender.hentSøknadForInnsender(fnrInnsender, søknadId, innsenderRolle)
        }
        if (søknad == null) {
            logg.warn { "En formidler forsøkte å hente søknad med id: $søknadId, men den er ikke tilgjengelig for formidler nå" }
            call.feilmelding(HttpStatusCode.NotFound, "Søknaden er ikke tilgjengelig for innlogget formidler")
        } else {
            logg.info { "Formidler hentet ut søknad med id: $søknadId" }
            call.respond(søknad)
        }
    }

    get<Søknader.Innsender.VenterGodkjenning> {
        val innbygger = call.autentisertInnbygger()
        val fnr = innbygger.fnr
        logg.info { "Henter formidlers saker som venter på godkjenning." }
        val saker = transaction {
            søknadStoreInnsender.hentBehovsmeldingerTilGodkjenningForInnsender(fnr)
        }
        call.respond(Response(saker))
    }

    suspend fun RoutingContext.brukerbekreftelse(it: Søknader.Bruker.SøknadId.Bekreftelse, utfall: BekreftelseUtfall) {
        logg.info { "Brukerbekreftelse: utfall=$utfall" }
        val søknadId = it.parent.søknadId
        val innbygger = call.autentisertInnbygger()

        // Verifiser status og bruker
        val søknad = transaction { søknadStore.hentSøknad(søknadId) }
        val søknadVenterPåGodkjenningFraGittBruker = when {
            søknad == null -> false
            søknad.fnrBruker != innbygger.fnr -> false
            else -> søknad.status == BehovsmeldingStatus.VENTER_GODKJENNING
        }
        if (!søknadVenterPåGodkjenningFraGittBruker) {
            return call.feilmelding(HttpStatusCode.BadRequest, "Søknad venter ikke på godkjenning fra denne brukeren")
        }

        // Send ut kafka event
        data class Event(
            override val eventId: UUID = UUID.randomUUID(),
            override val eventName: String,
            val soknadId: UUID,
        ) : KafkaMessage
        kafkaClient.send(innbygger.id.toString(), Event(eventName = utfall.eventName, soknadId = søknadId))
        call.respond(HttpStatusCode.NoContent)
    }
    post<Søknader.Bruker.SøknadId.Bekreftelse> { brukerbekreftelse(it, BekreftelseUtfall.GODKJENT_AV_BRUKER) }
    delete<Søknader.Bruker.SøknadId.Bekreftelse> { brukerbekreftelse(it, BekreftelseUtfall.SLETTET_AV_BRUKER) }

    patch<Behovsmelding.BehovsmeldingId.BrukerbekreftelseTilFullmakt> {
        val behovsmeldingId = it.parent.behovsmeldingId
        val innbygger = call.autentisertInnbygger()
        val innsenderFnr = innbygger.fnr
        søknadService.konverterBrukerbekreftelseTilFullmakt(behovsmeldingId, innsenderFnr)
        call.respond(HttpStatusCode.OK)
    }

    get<Behovsmelding.BehovsmeldingId.Status> {
        val behovsmeldingId = it.parent.behovsmeldingId
        val status = søknadService.hentStatus(behovsmeldingId)
        call.respond(HttpStatusCode.OK, StatusResponse(status))
    }

    get<Bruker.Dokumenter.ForSak> {
        val innbygger = call.autentisertInnbygger()
        val journalposter = safselvbetjeningClient
            .hentJournalposter(innbygger.userToken, innbygger.fnr, it.fagsakId)
            .map(::JournalpostJson)
        call.respond(journalposter)
    }

    get<Bruker.Dokumenter> {
        val innbygger = call.autentisertInnbygger()
        val journalposter = safselvbetjeningClient
            .hentJournalposter(innbygger.userToken, innbygger.fnr)
            .map(::JournalpostJson)
        call.respond(journalposter)
    }

    get<Bruker.ArkivDokumenter.JournalpostId.DokumentId.Dokumentvariant> {
        val innbygger = call.autentisertInnbygger()
        safselvbetjeningClient.hentDokument(
            userToken = innbygger.userToken,
            journalpostId = it.parent.parent.journalpostId,
            dokumentId = it.parent.dokumentId,
            dokumentvariant = it.dokumentvariant,
            upstreamHeaders = call.upstreamHeaders(),
        ) { upstream ->
            // headers.append("X-Frame-Options", "SAMEORIGIN") -> fixme -> trenger vi denne?
            call.respondUpstream(upstream)
        }
    }
}

private enum class BekreftelseUtfall(val eventName: String) {
    GODKJENT_AV_BRUKER("godkjentAvBruker"),
    SLETTET_AV_BRUKER("slettetAvBruker"),
}
