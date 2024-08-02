package no.nav.hjelpemidler.soknad.db.metrics

import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.configuration.InfluxDBEnvironmentVariable
import no.nav.hjelpemidler.configuration.NaisEnvironmentVariable
import no.nav.hjelpemidler.soknad.db.metrics.kafka.KafkaClient
import no.nav.hjelpemidler.soknad.db.metrics.kafka.createKafkaClient
import java.time.Instant

private val logg = KotlinLogging.logger {}

class InfluxDB {
    private val kafkaClient: KafkaClient = createKafkaClient()

    private val client = InfluxDBClientFactory.createV1(
        "${InfluxDBEnvironmentVariable.INFLUX_HOST}:${InfluxDBEnvironmentVariable.INFLUX_PORT}",
        InfluxDBEnvironmentVariable.INFLUX_USER,
        InfluxDBEnvironmentVariable.INFLUX_PASSWORD.toCharArray(),
        InfluxDBEnvironmentVariable.INFLUX_DATABASE_NAME,
        "default_retention_policy",
    ).makeWriteApi()

    private fun writeEvent(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
        val point = Point(measurement)
            .addTags(DEFAULT_TAGS)
            .addTags(tags)
            .addFields(fields)
            .time(Instant.now().toEpochMilli(), WritePrecision.MS)

        client.writePoint(point)
        kafkaClient.hendelseOpprettet(measurement, fields, tags)
        logg.debug { "Sendte hendelse til InfluxDB og BigQuery: '${point.toLineProtocol()}'" }
    }

    fun registerElapsedTime(metricFieldName: String, tid: Long) {
        writeEvent(metricFieldName, mapOf("elapsed_ms" to tid), emptyMap())
    }

    fun registerStatusCounts(metricFieldName: String, antallByStatus: Map<BehovsmeldingStatus, Int>) {
        writeEvent(metricFieldName, antallByStatus.mapKeys { it.key.toString() }, emptyMap())
    }

    companion object {
        private val DEFAULT_TAGS: Map<String, String> = mapOf(
            "application" to NaisEnvironmentVariable.NAIS_APP_NAME,
            "cluster" to NaisEnvironmentVariable.NAIS_CLUSTER_NAME,
            "namespace" to NaisEnvironmentVariable.NAIS_NAMESPACE,
        )
    }
}
