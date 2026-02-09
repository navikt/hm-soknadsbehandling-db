package no.nav.hjelpemidler.soknad.db.kafka

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

object LocalKafkaClient : KafkaClient {
    override fun hendelseOpprettet(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
        log.info { "hendelseOpprettet: $measurement, <${toEventString(measurement, fields, tags)}>" }
    }

    override fun <K, V> send(key: K, value: V) {
        log.info { "send: $key, $value" }
    }
}
