package no.nav.hjelpemidler.soknad.db.metrics.kafka

class LocalKafkaClient : KafkaClient {
    override fun hendelseOpprettet(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
        println("hendelseOpprettet: $measurement, <${toEventString(measurement, fields, tags)}>")
    }
}
