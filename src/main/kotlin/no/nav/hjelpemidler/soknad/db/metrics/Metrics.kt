package no.nav.hjelpemidler.soknad.db.metrics

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotliquery.Session
import kotliquery.queryOf
import mu.KotlinLogging
import no.nav.hjelpemidler.soknad.db.domain.Status
import java.sql.Timestamp
import java.util.UUID

private val logg = KotlinLogging.logger {}

class Metrics {

    private val aivenMetrics = AivenMetrics()
    private val sensuMetrics = SensuMetrics()

    fun measureElapsedTimeBetweenStatusChanges(session: Session, soknadsId: UUID, status: Status) {
        runBlocking {
            launch(Job()) {
                recordForStatus(session,
                    soknadsId,
                    status,
                    TID_FRA_VENTER_GODKJENNING_TIL_GODKJENT,
                    listOf(Status.VENTER_GODKJENNING),
                    listOf(Status.GODKJENT))
                recordForStatus(session,
                    soknadsId,
                    status,
                    TID_FRA_GODKJENT_TIL_JOURNALFORT,
                    listOf(Status.GODKJENT, Status.GODKJENT_MED_FULLMAKT),
                    listOf(Status.ENDELIG_JOURNALFØRT))
                recordForStatus(session,
                    soknadsId,
                    status,
                    TID_FRA_JOURNALFORT_TIL_VEDTAK,
                    listOf(Status.ENDELIG_JOURNALFØRT),
                    listOf(
                        Status.VEDTAKSRESULTAT_ANNET,
                        Status.VEDTAKSRESULTAT_AVSLÅTT,
                        Status.VEDTAKSRESULTAT_DELVIS_INNVILGET,
                        Status.VEDTAKSRESULTAT_INNVILGET,
                        Status.VEDTAKSRESULTAT_MUNTLIG_INNVILGET,
                        Status.VEDTAKSRESULTAT_HENLAGTBORTFALT,
                    ))
                recordForStatus(session,
                    soknadsId,
                    status,
                    TID_FRA_VEDTAK_TIL_UTSENDING,
                    listOf(
                        Status.VEDTAKSRESULTAT_ANNET,
                        Status.VEDTAKSRESULTAT_AVSLÅTT,
                        Status.VEDTAKSRESULTAT_DELVIS_INNVILGET,
                        Status.VEDTAKSRESULTAT_INNVILGET,
                        Status.VEDTAKSRESULTAT_MUNTLIG_INNVILGET,
                        Status.VEDTAKSRESULTAT_HENLAGTBORTFALT,
                    ),
                    listOf(Status.UTSENDING_STARTET))
            }
        }
    }

    private fun recordForStatus(
        session: Session,
        soknadsId: UUID,
        status: Status,
        metricFieldName: String,
        validStartStatuses: List<Status>,
        validEndStatuses: List<Status>,
    ) {
        try {
            if (status in validEndStatuses) {
                data class StatusRow(val STATUS: Status, val CREATED: Timestamp, val ER_DIGITAL: Boolean)

                val result = session.run(
                    queryOf(
                        "SELECT STATUS, V1_STATUS.CREATED AS CREATED, ER_DIGITAL FROM V1_STATUS JOIN V1_SOKNAD ON V1_STATUS.SOKNADS_ID = V1_SOKNAD.SOKNADS_ID WHERE V1_STATUS.SOKNADS_ID = ? ORDER BY CREATED DESC;", // ORDER is just a preventative measure
                        soknadsId
                    ).map {
                        StatusRow(
                            Status.valueOf(it.string("STATUS")),
                            it.sqlTimestamp("CREATED"),
                            it.boolean("ER_DIGITAL"),
                        )
                    }.asList
                )

                // TODO if multiple statuses converged to a single common status this would not be necessary
                val foundEndStatuses = result.filter { statusRow -> statusRow.STATUS in validEndStatuses }
                if (foundEndStatuses.isEmpty()) return
                val foundStartStatuses = result.filter { statusRow -> statusRow.STATUS in validStartStatuses }
                if (foundStartStatuses.isEmpty()) return

                val earliestEndStatus = foundEndStatuses.minByOrNull { statusRow -> statusRow.CREATED } ?: return
                val earliestStartStatus = foundStartStatuses.minByOrNull { statusRow -> statusRow.CREATED } ?: return

                val timeDifference = earliestEndStatus.CREATED.time - earliestStartStatus.CREATED.time

                val finalMetricFieldName =
                    if (foundEndStatuses[0].ER_DIGITAL) metricFieldName else metricFieldName.plus("-papir")

                aivenMetrics.registerElapsedTime(finalMetricFieldName, timeDifference)
                sensuMetrics.registerElapsedTime(finalMetricFieldName, timeDifference)
            }
        } catch (e: Exception) {
            logg.error(e) { "Feil ved sending av tid mellom status-metrikker" }
        }
    }

    fun countApplicationsByStatus(session: Session) {
        runBlocking {
            launch(Job()) {
                try {
                    data class StatusRow(val STATUS: Status, val COUNT: Number)

                    val result = session.run(
                        queryOf(
                            """
                                SELECT STATUS AS STATUS, COUNT(SOKNADS_ID) AS COUNT FROM (
                                SELECT V1_STATUS.SOKNADS_ID, V1_STATUS.STATUS FROM V1_STATUS
                                                    LEFT JOIN V1_STATUS last_status ON
                                            V1_STATUS.SOKNADS_ID = last_status.SOKNADS_ID AND
                                            V1_STATUS.created < last_status.created
                                WHERE last_status.SOKNADS_ID IS NULL) last_statuses
                                GROUP BY STATUS                                
                            """.trimIndent()
                        ).map {
                            StatusRow(
                                Status.valueOf(it.string("STATUS")),
                                it.int("COUNT"),
                            )
                        }.asList
                    )

                    val metricsToSend =
                        result.associate { statusRow -> let { statusRow.STATUS.toString() to statusRow.COUNT.toInt() } }

                    if (!metricsToSend.isEmpty())
                        aivenMetrics.registerStatusCounts(COUNT_OF_SOKNAD_BY_STATUS, metricsToSend)
                } catch (e: Exception) {
                    logg.error { "Feil ved sending antall per status metrikker: ${e.message}. ${e.stackTrace}" }
                }
            }
        }
    }

    companion object {
        const val TID_FRA_VENTER_GODKJENNING_TIL_GODKJENT =
            "hm-soknadsbehandling.event.tid_fra_venter_godkjenning_til_godkjent"
        const val TID_FRA_GODKJENT_TIL_JOURNALFORT = "hm-soknadsbehandling.event.tid_fra_godkjent_til_journalfort"
        const val TID_FRA_JOURNALFORT_TIL_VEDTAK = "hm-soknadsbehandling.event.tid_fra_journalfort_til_vedtak"
        const val TID_FRA_VEDTAK_TIL_UTSENDING = "hm-soknadsbehandling.event.tid_fra_vedtak_til_utsending"
        const val COUNT_OF_SOKNAD_BY_STATUS = "hm-soknadsbehandling.event.count_of_soknad_by_status"
    }
}
