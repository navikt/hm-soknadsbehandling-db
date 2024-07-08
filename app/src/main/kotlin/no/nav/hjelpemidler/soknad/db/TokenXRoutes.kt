package no.nav.hjelpemidler.soknad.db

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.ktor.søknadId
import no.nav.hjelpemidler.soknad.db.ordre.OrdreService
import no.nav.hjelpemidler.soknad.db.rolle.RolleService
import no.nav.hjelpemidler.soknad.db.store.Transaction
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import java.security.MessageDigest
import java.time.LocalDate
import java.util.Date

private val logg = KotlinLogging.logger {}

fun Route.tokenXRoutes(
    transaction: Transaction,
    ordreService: OrdreService,
    rolleService: RolleService,
    tokenXUserFactory: TokenXUserFactory = TokenXUserFactory,
) {
    get("/soknad/bruker/{soknadId}") {
        try {
            val søknadId = call.søknadId
            val fnr = tokenXUserFactory.createTokenXUser(call).ident
            val soknad = transaction { søknadStore.hentSoknad(søknadId) }

            when {
                soknad == null -> {
                    call.respond(HttpStatusCode.NotFound)
                }

                soknad.fnrBruker != fnr -> {
                    call.respond(HttpStatusCode.Forbidden, "Søknad er ikke registrert på aktuell bruker")
                }

                else -> {
                    // Fetch ordrelinjer belonging to søknad
                    soknad.ordrelinjer = ordreService.finnOrdreForSøknad(soknad.søknadId)

                    // Fetch fagsakid if it exists
                    val fagsakData = transaction { infotrygdStore.hentFagsakIdForSøknad(soknad.søknadId) }
                    if (fagsakData != null) {
                        soknad.fagsakId = fagsakData.fagsakId
                    } else {
                        val fagsakData2 = transaction { hotsakStore.hentFagsakIdForSøknad(soknad.søknadId) }
                        if (fagsakData2 != null) soknad.fagsakId = fagsakData2
                    }

                    // Fetch soknadType for søknad
                    soknad.søknadType = transaction { infotrygdStore.hentTypeForSøknad(soknad.søknadId) }

                    call.respond(soknad)
                }
            }
        } catch (e: Exception) {
            logg.error(e) { "Feilet ved henting av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad")
        }
    }

    get("/soknad/bruker") {
        val fnr = tokenXUserFactory.createTokenXUser(call).ident

        try {
            val brukersSaker = transaction { søknadStore.hentSoknaderForBruker(fnr) }
            call.respond(brukersSaker)
        } catch (e: Exception) {
            logg.error(e) { "Error on fetching søknader til godkjenning" }
            call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av saker")
        }
    }

    get("/soknad/innsender") {
        val user = tokenXUserFactory.createTokenXUser(call)
        val fnrInnsender = user.ident
        val innsenderRolle = rolleService.hentRolle(user.tokenString)

        try {
            val formidlersSøknader = transaction {
                søknadStoreInnsender.hentSøknaderForInnsender(fnrInnsender, innsenderRolle)
            }

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
                logg.info("Formidlersiden ble lastet inn med sak(er) eldre enn 6mnd.: id=$hash, tilfeller=${datoer.count()} stk., datoOpprettet(siste 10): $lastTen.")
            }

            call.respond(formidlersSøknader)
        } catch (e: Exception) {
            logg.error(e) { "Error on fetching formidlers søknader" }
            call.respond(HttpStatusCode.InternalServerError, e)
        }
    }

    get("/soknad/innsender/{soknadId}") {
        val søknadId = call.søknadId
        val user = tokenXUserFactory.createTokenXUser(call)
        val fnrInnsender = user.ident
        val innsenderRolle = rolleService.hentRolle(user.tokenString)

        try {
            val formidlersSoknad = transaction {
                søknadStoreInnsender.hentSøknadForInnsender(fnrInnsender, søknadId, innsenderRolle)
            }
            if (formidlersSoknad == null) {
                logg.warn { "En formidler forsøkte å hente søknad <$søknadId>, men den er ikke tilgjengelig for formidler nå" }
                call.respond(status = HttpStatusCode.NotFound, "Søknaden er ikke tilgjengelig for innlogget formidler")
            } else {
                logg.info { "Formidler hentet ut søknad $søknadId" }
                call.respond(formidlersSoknad)
            }
        } catch (e: Exception) {
            logg.error(e) { "Error on fetching formidlers søknader" }
            call.respond(HttpStatusCode.InternalServerError, e)
        }
    }

    get("/validerSøknadsidOgStatusVenterGodkjenning/{soknadId}") {
        try {
            val søknadId = call.søknadId
            val fnr = tokenXUserFactory.createTokenXUser(call).ident
            val soknad = transaction { søknadStore.hentSoknad(søknadId) }

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
            logg.error(e) { "Feilet ved henting av søknad" }
            call.respond(HttpStatusCode.BadRequest, "Feilet ved henting av søknad")
        }
    }
}
