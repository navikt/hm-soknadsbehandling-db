package no.nav.hjelpemidler.soknad.db.metrics

import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.hjelpemidler.soknad.db.Configuration
import java.time.Instant

private val logg = KotlinLogging.logger {}
// private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class AivenMetrics {
    private val influxHost = Configuration.application.INFLUX_HOST ?: "http://localhost"
    private val influxPort = Configuration.application.INFLUX_PORT ?: "1234"
    private val influxDatabaseName = Configuration.application.INFLUX_DATABASE_NAME ?: "defaultdb"
    private val influxUser = Configuration.application.INFLUX_USER ?: "user"
    private val influxPassword = Configuration.application.INFLUX_PASSWORD ?: "password"

    private val client = InfluxDBClientFactory.createV1(
        "$influxHost:$influxPort",
        influxUser,
        influxPassword.toCharArray(),
        influxDatabaseName,
        "default_retention_policy"
    ).makeWriteApi()

    fun writeEvent(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) = runBlocking {
        // TODO: Get nanoseconds
        val point = Point(measurement)
            .addTags(DEFAULT_TAGS)
            .addTags(tags)
            .addFields(fields)
            .time(Instant.now().toEpochMilli(), WritePrecision.MS)

        client.writePoint(point)
        logg.info("Skriv point-objekt til Aiven: ${point.toLineProtocol()}")
    }

    fun registerElapsedTime(metricFieldName: String, tid: Long) {
        writeEvent(metricFieldName, mapOf("elapsed_ms" to tid), emptyMap())
    }

    fun registerStatusCounts(metricFieldName: String, statusCounts: Map<String, Any>) {
        writeEvent(metricFieldName, statusCounts, emptyMap())
    }

    companion object {
        private val DEFAULT_TAGS: Map<String, String> = mapOf(
            "application" to (Configuration.application.NAIS_APP_NAME ?: "hm-soknadsbehandling-db"),
            "cluster" to (Configuration.application.NAIS_CLUSTER_NAME ?: "dev-gcp"),
            "namespace" to (Configuration.application.NAIS_NAMESPACE ?: "teamdigihot")
        )
    }
}
