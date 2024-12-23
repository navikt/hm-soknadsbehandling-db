package no.nav.hjelpemidler.soknad.db.metrics.kafka

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

object LocalKafkaClient : KafkaClient {
    override fun hendelseOpprettet(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
        log.info { "hendelseOpprettet: $measurement, <${toEventString(measurement, fields, tags)}>" }
    }
}
