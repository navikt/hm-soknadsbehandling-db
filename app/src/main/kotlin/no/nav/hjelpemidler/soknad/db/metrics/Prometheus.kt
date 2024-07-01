package no.nav.hjelpemidler.soknad.db.metrics

import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

internal object Prometheus {
    val registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val dbTimer: Timer = registry.timer("hm_soknad_mottak_db_query_latency_histogram")
}
