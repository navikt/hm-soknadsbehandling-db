package no.nav.hjelpemidler.soknad.db.kafka

import io.ktor.server.plugins.di.DependencyRegistry
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.configuration.GcpEnvironment

fun DependencyRegistry.kafka() {
    provide<KafkaClient> {
        if (Environment.current is GcpEnvironment) {
            DefaultKafkaClient()
        } else {
            LocalKafkaClient
        }
    }
}
