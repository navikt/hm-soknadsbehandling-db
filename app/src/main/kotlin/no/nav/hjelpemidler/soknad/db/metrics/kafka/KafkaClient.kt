package no.nav.hjelpemidler.soknad.db.metrics.kafka

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.configuration.LocalEnvironment
import java.time.LocalDateTime
import java.util.UUID

interface KafkaClient {
    fun hendelseOpprettet(measurement: String, fields: Map<String, Any>, tags: Map<String, String>)

    fun toEventString(measurement: String, fields: Map<String, Any>, tags: Map<String, String>): String =
        mapper.writeValueAsString(
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
}

fun createKafkaClient() = when (Environment.current) {
    LocalEnvironment -> LocalKafkaClient()
    else -> AivenKafkaClient()
}

private val mapper = jacksonMapperBuilder()
    .addModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .serializationInclusion(JsonInclude.Include.NON_NULL)
    .build()
