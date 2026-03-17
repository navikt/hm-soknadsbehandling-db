package no.nav.hjelpemidler.soknad.db.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.soknad.db.kafka.KafkaClient

private val logg = KotlinLogging.logger {}

class BigQueryClient(
    val kafkaClient: KafkaClient,
) {
    fun writeEvent(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) = try {
        kafkaClient.hendelseOpprettet(measurement, fields, tags)
    } catch (e: Exception) {
        logg.error(e) { "Feil ved oppretting av måling: $measurement" }
    }

    fun registerElapsedTime(metricFieldName: String, tid: Long) {
        writeEvent(metricFieldName, mapOf("elapsed_ms" to tid), emptyMap())
    }

    fun registerStatusCounts(metricFieldName: String, antallByStatus: Map<BehovsmeldingStatus, Int>) {
        writeEvent(metricFieldName, antallByStatus.mapKeys { it.key.toString() }, emptyMap())
    }
}
