package no.nav.hjelpemidler.soknad.db.metrics

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Histogram

internal object Prometheus {
    val collectorRegistry = CollectorRegistry.defaultRegistry

    val dbTimer = Histogram.build("hm_soknad_mottak_db_query_latency_histogram", "Distribution of db execution times")
        .labelNames("query")
        .register(collectorRegistry)
}
