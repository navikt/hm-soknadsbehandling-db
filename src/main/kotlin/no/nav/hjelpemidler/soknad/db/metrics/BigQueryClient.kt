package no.nav.hjelpemidler.soknad.db.metrics

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.Clustering
import com.google.cloud.bigquery.DatasetId
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import com.google.cloud.bigquery.TimePartitioning
import mu.KotlinLogging
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

interface BigQueryClient {
    fun hendelseOpprettet(
        measurement: String,
        fields: Map<String, Any>,
        tags: Map<String, String>,
    )
}

class DefaultBigQueryClient(datasetId: DatasetId) : BigQueryClient {
    private val bq: BigQuery = BigQueryOptions.newBuilder()
        .setProjectId(datasetId.project)
        .build()
        .service
    private val tableId: TableId = TableId.of(datasetId.dataset, "hendelse_v1")

    init {
        when (bq.getTable(tableId)) {
            null -> {
                val schema = Schema.of(listOf(
                    Field.newBuilder("opprettet", StandardSQLTypeName.DATETIME)
                        .setMode(Field.Mode.REQUIRED)
                        .build(),
                    Field.newBuilder("navn", StandardSQLTypeName.STRING)
                        .setMode(Field.Mode.REQUIRED)
                        .build(),
                    Field
                        .newBuilder(
                            "data", StandardSQLTypeName.STRUCT,
                            Field.newBuilder("navn", StandardSQLTypeName.STRING)
                                .setMode(Field.Mode.REQUIRED)
                                .build(),
                            Field.newBuilder("verdi", StandardSQLTypeName.STRING)
                                .setMode(Field.Mode.REQUIRED)
                                .build(),
                        )
                        .setMode(Field.Mode.REPEATED)
                        .build(),
                    Field.newBuilder("tidsstempel", StandardSQLTypeName.TIMESTAMP)
                        .setMode(Field.Mode.REQUIRED)
                        .build(),
                ))

                val tableInfo = TableInfo.of(
                    tableId,
                    StandardTableDefinition.newBuilder()
                        .setSchema(schema)
                        .setTimePartitioning(TimePartitioning.newBuilder(TimePartitioning.Type.MONTH)
                            .setField("opprettet")
                            .build())
                        .setClustering(Clustering.newBuilder()
                            .setFields(listOf("opprettet"))
                            .build())
                        .build()
                )
                bq.create(tableInfo)
            }
            else -> {
                log.info { "Tabell hendelse_v1 eksisterer allerede" }
            }
        }
    }

    override fun hendelseOpprettet(
        measurement: String,
        fields: Map<String, Any>,
        tags: Map<String, String>,
    ) {
        val table = requireNotNull(bq.getTable(tableId)) { "Fant ikke tabellen hendelse_v1" }
        val response = table.insert(
            listOf(
                InsertAllRequest.RowToInsert.of(
                    mapOf(
                        "opprettet" to LocalDateTime.now().truncatedTo(ChronoUnit.MICROS).toString(),
                        "navn" to measurement,
                        "data" to fields.mapValues { it.value.toString() }
                            .plus(tags)
                            .filterKeys { it != "counter" }
                            .map {
                                mapOf("navn" to it.key, "verdi" to it.value)
                            },
                        "tidsstempel" to "AUTO"
                    )
                )
            )
        )
        if (response.hasErrors()) {
            log.error { "Feil under insert av rad til BigQuery, errors: ${response.insertErrors}" }
        }
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}

class LocalBigQueryClient : BigQueryClient {
    override fun hendelseOpprettet(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
        log.info { "hendelseOpprettet(measurement, fields, tags) kalt med $measurement, $fields, $tags" }
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
