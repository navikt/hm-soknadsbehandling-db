package no.nav.hjelpemidler.soknad.db

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSak
import no.nav.hjelpemidler.domain.person.Fødselsnummer
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.kafka.Melding
import no.nav.hjelpemidler.soknad.db.ktor.Response
import no.nav.hjelpemidler.soknad.db.safselvbetjening.Bruker
import no.nav.hjelpemidler.soknad.db.soknad.Behovsmelding
import no.nav.hjelpemidler.soknad.db.soknad.Søknader
import no.nav.hjelpemidler.soknad.db.store.Transaction
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.UUID

private val logg = KotlinLogging.logger {}

fun Route.tokenXRoutes(
    transaction: Transaction,
    serviceContext: ServiceContext,
    tokenXUserFactory: TokenXUserFactory = TokenXUserFactory,
) {
    val ordreService = serviceContext.ordreService
    val rolleService = serviceContext.rolleService
    val søknadService = serviceContext.søknadService
    val safselvbetjening = serviceContext.safselvbetjening
    val kafkaClient = serviceContext.kafkaClient

    get<Søknader.Bruker.SøknadId> {
        val fnr = tokenXUserFactory.createTokenXUser(call).ident
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
        val fnr = tokenXUserFactory.createTokenXUser(call).ident
        val brukersSaker = transaction { søknadStore.hentSøknaderForBruker(fnr) }
        call.respond(brukersSaker)
    }

    get<Søknader.Innsender> {
        val user = tokenXUserFactory.createTokenXUser(call)
        val fnrInnsender = user.ident
        val innsenderRolle = rolleService.hentRolle(user.tokenString)

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
        val user = tokenXUserFactory.createTokenXUser(call)
        val fnrInnsender = user.ident
        val innsenderRolle = rolleService.hentRolle(user.tokenString)

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
        val fnr = tokenXUserFactory.createTokenXUser(call).ident
        logg.info { "Henter formidlers saker som venter på godkjenning." }
        val saker = transaction {
            søknadStoreInnsender.hentBehovsmeldingerTilGodkjenningForInnsender(fnr)
        }
        call.respond(Response(saker))
    }

    suspend fun RoutingContext.brukerbekreftelse(it: Søknader.Bruker.SøknadId.Bekreftelse, utfall: BekreftelseUtfall) {
        logg.info { "Brukerbekreftelse: utfall=$utfall" }
        val søknadId = it.parent.søknadId
        val user = tokenXUserFactory.createTokenXUser(call)

        // Verifiser status og bruker
        val søknad = transaction { søknadStore.hentSøknad(søknadId) }
        val søknadVenterPåGodkjenningFraGittBruker = when {
            søknad == null -> false
            søknad.fnrBruker != user.ident -> false
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
        ) : Melding
        kafkaClient.send(user.ident, Event(eventName = utfall.eventName, soknadId = søknadId))
        call.respond(HttpStatusCode.NoContent)
    }
    post<Søknader.Bruker.SøknadId.Bekreftelse> { brukerbekreftelse(it, BekreftelseUtfall.GODKJENT_AV_BRUKER) }
    delete<Søknader.Bruker.SøknadId.Bekreftelse> { brukerbekreftelse(it, BekreftelseUtfall.SLETTET_AV_BRUKER) }

    post<Behovsmelding.BehovsmeldingId.BrukerbekreftelseTilFullmakt> {
        val behovsmeldingId = it.parent.behovsmeldingId
        val innsenderFnr = tokenXUserFactory.createTokenXUser(call).fnr()
        søknadService.konverterBrukerbekreftelseToFullmakt(behovsmeldingId, innsenderFnr)
        call.respond(HttpStatusCode.OK)
    }

    get<Bruker.Dokumenter.ForSak> {
        val user = tokenXUserFactory.createTokenXUser(call)
        val fnr = user.ident
        val token = user.tokenString!!
        val results = safselvbetjening.hentDokumenter(token, fnr, it.fagsakId)
        call.respond(results)
    }

    get<Bruker.Dokumenter> {
        val user = tokenXUserFactory.createTokenXUser(call)
        val fnr = user.ident
        val token = user.tokenString!!
        val results = safselvbetjening.hentDokumenter(token, fnr)
        call.respond(results)
    }

    get<Bruker.ArkivDokumenter.JournalpostId.DokumentId.Dokumentvariant> {
        val user = tokenXUserFactory.createTokenXUser(call)
        val token = user.tokenString!!
        safselvbetjening.hentPdfDokumentProxy(
            token,
            call,
            it.parent.parent.journalpostId,
            it.parent.dokumentId,
            it.dokumentvariant,
        )
    }
}

private enum class BekreftelseUtfall(val eventName: String) {
    GODKJENT_AV_BRUKER("godkjentAvBruker"),
    SLETTET_AV_BRUKER("slettetAvBruker"),
}

fun TokenXUser.fnr(): Fødselsnummer = Fødselsnummer(ident)
