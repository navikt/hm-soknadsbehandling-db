package no.nav.hjelpemidler.soknad.db.metrics.kafka

import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.configuration.LocalEnvironment
import no.nav.hjelpemidler.configuration.TestEnvironment
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import java.time.LocalDateTime
import java.util.UUID

interface KafkaClient {
    fun hendelseOpprettet(measurement: String, fields: Map<String, Any>, tags: Map<String, String>)

    fun toEventString(measurement: String, fields: Map<String, Any>, tags: Map<String, String>): String = jsonMapper.writeValueAsString(
        mapOf(
            "eventId" to UUID.randomUUID(),
            "eventName" to "hm-bigquery-sink-hendelse",
            "schemaId" to "hendelse_v2",
            "payload" to mapOf(
                "opprettet" to LocalDateTime.now(),
                "navn" to measurement,
                "kilde" to "hm-soknadsbehandling-db",
                "data" to fields.mapValues { it.value.toString() }
                    .plus(tags)
                    .filterKeys { it != "counter" },
            ),
        ),
    )

    fun <K, V> send(key: K, value: V)
}

fun createKafkaClient(): KafkaClient = when (Environment.current) {
    LocalEnvironment, TestEnvironment -> LocalKafkaClient
    else -> AivenKafkaClient()
}
