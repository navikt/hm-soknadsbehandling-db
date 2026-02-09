package no.nav.hjelpemidler.soknad.db.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.kafka.createKafkaProducer
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.soknad.db.Configuration
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.concurrent.TimeUnit

private val logg = KotlinLogging.logger { }

class AivenKafkaClient : KafkaClient {
    private val topic = Configuration.KAFKA_TOPIC
    private val kafkaProducer: Producer<String, String> = createKafkaProducer()

    private fun produceEvent(key: String, event: String) {
        try {
            kafkaProducer.send(ProducerRecord(topic, key, event)).get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            // Svelger exceptions fordi sending av statistikk ikke er kritisk. Log error slik at noen forhåpentligvis oppdager og fikser problemet.
            logg.error(e) { "Sending til topic: $topic feilet for event: '$event'" }
        }
    }

    override fun hendelseOpprettet(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
        produceEvent(measurement, toEventString(measurement, fields, tags))
    }

    override fun <K, V> send(key: K, value: V) {
        produceEvent(key.toString(), jsonMapper.writeValueAsString(value))
    }
}
