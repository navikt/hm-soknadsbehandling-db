package no.nav.hjelpemidler.soknad.db.metrics

import io.ktor.server.plugins.di.DependencyRegistry
import io.ktor.server.plugins.di.provide

fun DependencyRegistry.metrics() {
    provide(::BigQueryClient)
    provide(::Metrics)
}
