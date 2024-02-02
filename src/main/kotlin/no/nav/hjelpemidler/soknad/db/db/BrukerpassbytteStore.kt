package no.nav.hjelpemidler.soknad.db.db

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.hjelpemidler.soknad.db.domain.BrukerpassbytteData
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.metrics.Prometheus
import org.postgresql.util.PGobject
import java.util.UUID
import javax.sql.DataSource

private val logg = KotlinLogging.logger {}

internal interface BrukerpassbytteStore {
    fun save(data: BrukerpassbytteData): Int
}

internal class BrukerpassbytteStorePostgres(private val ds: DataSource) : BrukerpassbytteStore {

    override fun save(data: BrukerpassbytteData): Int =
        time("insert_brukerpassbytte") {
            using(sessionOf(ds)) { session ->
                session.transaction { transaction ->
                    // Add the new status to the status table
                    if (!checkIfLastStatusMatches(transaction, data.id, data.status)) transaction.run(
                        queryOf(
                            "INSERT INTO V1_STATUS (SOKNADS_ID, STATUS) VALUES (?, ?)",
                            data.id,
                            data.status.name,
                        ).asUpdate
                    )
                    // Add the new Brukerpassbytte into the brukerpassbytte table
                    transaction.run(
                        queryOf(
                            """
                                INSERT INTO v1_brukerpassbytte (id, fnr_bruker, data, bytte_gjelder) 
                                VALUES (:id, :fnr_bruker, :data, :bytte_gjelder) 
                                ON CONFLICT DO NOTHING
                            """.trimIndent(), mapOf(
                                "id" to data.id,
                                "fnr_bruker" to data.fnr,
                                "data" to PGobject().apply {
                                    type = "jsonb"
                                    value = objectMapper.writeValueAsString(data.brukerpassbytte)
                                },
                                "bytte_gjelder" to data.soknadGjelder
                            )
                        ).asUpdate
                    )
                }
            }
        }

    private fun checkIfLastStatusMatches(session: Session, brukerpassbytteId: UUID, status: Status): Boolean {
        val result = session.run(
            queryOf(
                // TODO trenger vi 2 lag med select her? Kan vi ta SELECT STATUS direkte?
                "SELECT STATUS FROM V1_STATUS WHERE ID = (SELECT STATUS FROM V1_STATUS WHERE SOKNADS_ID = ? ORDER BY created DESC LIMIT 1)",
                brukerpassbytteId
            ).map {
                it.stringOrNull("STATUS")
            }.asSingle
        ) ?: return false /* special case where there is no status in the database (søknad is being added now) */
        return result == status.name
    }


    private inline fun <T : Any?> time(queryName: String, function: () -> T) =
        Prometheus.dbTimer.labels(queryName).startTimer().let { timer ->
            function().also {
                timer.observeDuration()
            }
        }

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

}
