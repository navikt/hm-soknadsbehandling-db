package no.nav.hjelpemidler.soknad.db.metrics

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.DatasetId
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
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
    private val bq = BigQueryOptions.newBuilder()
        .setProjectId(datasetId.project)
        .build()
        .service
    private val tableId = TableId.of(datasetId.dataset, "hendelse_v1")

    init {
        when (bq.getTable(tableId)) {
            null -> {
                val tableInfo = TableInfo.of(
                    tableId,
                    StandardTableDefinition.of(
                        Schema.of(
                            listOf(
                                Field.newBuilder("opprettet", StandardSQLTypeName.DATETIME).setMode(Field.Mode.REQUIRED).build(),
                                Field.newBuilder("navn", StandardSQLTypeName.STRING).setMode(Field.Mode.REQUIRED).build(),
                                Field.newBuilder(
                                    "data", StandardSQLTypeName.STRUCT,
                                    Field.of("navn", StandardSQLTypeName.STRING),
                                    Field.of("verdi", StandardSQLTypeName.STRING),
                                ).setMode(Field.Mode.REPEATED).build(),
                                Field.newBuilder("tidsstempel", StandardSQLTypeName.TIMESTAMP).setMode(Field.Mode.REQUIRED).build(),
                            )
                        )
                    )
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
        table.insert(
            listOf(
                InsertAllRequest.RowToInsert.of(
                    mapOf(
                        "opprettet" to LocalDateTime.now().truncatedTo(ChronoUnit.MICROS).toString(),
                        "navn" to measurement,
                        "data" to fields.mapValues { it.value.toString() }
                            .plus(tags)
                            .filterKeys { it != "counter" },
                        "tidsstempel" to "AUTO"
                    )
                )
            )
        )
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}

class LocalBigQueryClient : BigQueryClient {
    override fun hendelseOpprettet(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
    }
}
