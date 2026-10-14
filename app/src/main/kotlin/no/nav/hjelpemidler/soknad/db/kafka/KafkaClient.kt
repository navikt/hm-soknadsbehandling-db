package no.nav.hjelpemidler.soknad.db.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.kafka.BigQueryHendelse
import no.nav.hjelpemidler.kafka.sendAsync
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.soknad.db.Configuration
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDateTime

private val log = KotlinLogging.logger { }

class KafkaClient(private val producer: KafkaProducer) : AutoCloseable by producer {
    private val topic = Configuration.KAFKA_TOPIC

    suspend fun hendelseOpprettet(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
        send(
            measurement,
            BigQueryHendelse(
                schemaId = "hendelse_v2",
                payload = mapOf(
                    "opprettet" to LocalDateTime.now(),
                    "navn" to measurement,
                    "kilde" to "hm-soknadsbehandling-db",
                    "data" to fields.mapValues { it.value.toString() }
                        .plus(tags)
                        .filterKeys { it != "counter" },
                ),
            ),
        )
    }

    suspend fun <K, V> send(key: K, value: V) {
        try {
            producer.sendAsync(ProducerRecord(topic, key.toString(), jsonMapper.writeValueAsString(value)))
        } catch (e: Exception) {
            log.error(e) { "Publisering til Kafka feilet" }
            throw e
        }
    }
}
