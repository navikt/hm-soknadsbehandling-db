package no.nav.hjelpemidler.soknad.db.sak

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.resources.get
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSakId
import no.nav.hjelpemidler.soknad.db.store.Transaction

private val log = KotlinLogging.logger {}

fun Route.sakApi() {
    val transaction: Transaction by application.dependencies

    /*
     * NB! Kun saker fra Hotsak har en entydig sakId.
     */
    get<Saker.SakId> {
        val sakId = HotsakSakId(it.sakId)
        val sak = transaction { hotsakStore.finnSak(sakId) }
        if (sak == null) {
            log.info { "Fant ikke sak med sakId: $sakId" }
        }
        call.respondNullable(sak)
    }

    /*
     * NB! Kun saker fra Hotsak har en entydig sakId.
     */
    get<Saker.SakId.Søknad> {
        val sakId = HotsakSakId(it.parent.sakId)
        val søknad = transaction {
            val sak = hotsakStore.finnSak(sakId) ?: return@transaction null
            søknadStore.finnSøknad(sak.søknadId)
        }
        if (søknad == null) {
            log.info { "Fant ikke søknad for sakId: $sakId" }
        }
        call.respondNullable(søknad)
    }
}
