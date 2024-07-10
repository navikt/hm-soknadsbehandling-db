package no.nav.hjelpemidler.soknad.db

import io.ktor.server.routing.Route
import no.nav.hjelpemidler.soknad.db.metrics.Metrics
import no.nav.hjelpemidler.soknad.db.store.Transaction

fun Route.søknadApi(
    transaction: Transaction,
    metrics: Metrics,
) {
}
