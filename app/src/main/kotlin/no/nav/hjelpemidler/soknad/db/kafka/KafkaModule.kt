package no.nav.hjelpemidler.soknad.db.kafka

import io.ktor.server.plugins.di.DependencyRegistry
import io.ktor.server.plugins.di.provide
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.configuration.GcpEnvironment
import no.nav.hjelpemidler.kafka.createKafkaProducer
import no.nav.hjelpemidler.kafka.createMockProducer
import org.apache.kafka.clients.producer.Producer

typealias KafkaProducer = Producer<String, String>

fun DependencyRegistry.kafka() {
    provide<KafkaProducer> {
        if (Environment.current is GcpEnvironment) {
            createKafkaProducer()
        } else {
            createMockProducer(autoComplete = true)
        }
    }
    provide(::KafkaClient)
}
