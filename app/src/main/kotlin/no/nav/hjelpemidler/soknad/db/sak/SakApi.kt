package no.nav.hjelpemidler.soknad.db.sak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSakId
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.store.Transaction

fun Route.sakApi(
    transaction: Transaction,
) {
    /**
     * NB! Kun saker fra Hotsak har en entydig sakId.
     */
    get<Saker.SakId> {
        val sakId = HotsakSakId(it.sakId)
        val sak = transaction {
            hotsakStore.finnSak(sakId)
        } ?: return@get call.feilmelding(HttpStatusCode.NotFound, "Fant ikke sak med sakId: $sakId")
        call.respond(sak)
    }

    /**
     * NB! Kun saker fra Hotsak har en entydig sakId.
     */
    get<Saker.SakId.Søknad> {
        val sakId = HotsakSakId(it.parent.sakId)
        val søknad = transaction {
            val sak = hotsakStore.finnSak(sakId) ?: return@transaction null
            søknadStore.finnSøknad(sak.søknadId)
        } ?: return@get call.feilmelding(HttpStatusCode.NotFound, "Fant ikke søknad for sakId: $sakId")
        call.respond(søknad)
    }
}
