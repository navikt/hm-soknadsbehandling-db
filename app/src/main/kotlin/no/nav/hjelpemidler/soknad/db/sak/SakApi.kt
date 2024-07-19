package no.nav.hjelpemidler.soknad.db.sak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.store.Transaction

fun Route.sakApi(
    transaction: Transaction,
) {
    /**
     * NB! Kun saker fra Hotsak har en entydig sakId.
     */
    get<Saker.SakId> {
        val sakId = it.sakId
        val sak = transaction {
            hotsakStore.finnSak(HotsakSakId(sakId))
        } ?: return@get call.feilmelding(HttpStatusCode.NotFound)
        call.respond(sak)
    }
}
