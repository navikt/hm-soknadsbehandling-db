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

    fun measureElapsedTimeBetweenStatusChanges(session: Session, soknadsId: UUID, status: Status) {
        runBlocking {
            launch(Job()) {
                recordForStatus(session, soknadsId, status, TID_FRA_VENTER_GODKJENNING_TIL_GODKJENT, listOf(Status.VENTER_GODKJENNING), listOf(Status.GODKJENT))
                recordForStatus(session, soknadsId, status, TID_FRA_GODKJENT_TIL_JOURNALFORT, listOf(Status.GODKJENT, Status.GODKJENT_MED_FULLMAKT), listOf(Status.ENDELIG_JOURNALFØRT))
                recordForStatus(session, soknadsId, status, TID_FRA_JOURNALFORT_TIL_VEDTAK, listOf(Status.ENDELIG_JOURNALFØRT), listOf(Status.VEDTAKSRESULTAT_ANNET, Status.VEDTAKSRESULTAT_AVSLÅTT, Status.VEDTAKSRESULTAT_DELVIS_INNVILGET, Status.VEDTAKSRESULTAT_INNVILGET, Status.VEDTAKSRESULTAT_MUNTLIG_INNVILGET))
                recordForStatus(session, soknadsId, status, TID_FRA_VEDTAK_TIL_UTSENDING, listOf(Status.VEDTAKSRESULTAT_ANNET, Status.VEDTAKSRESULTAT_AVSLÅTT, Status.VEDTAKSRESULTAT_DELVIS_INNVILGET, Status.VEDTAKSRESULTAT_INNVILGET, Status.VEDTAKSRESULTAT_MUNTLIG_INNVILGET), listOf(Status.UTSENDING_STARTET))
            }
        }
    }

    private fun recordForStatus(session: Session, soknadsId: UUID, status: Status, metricFieldName: String, validStartStatuses: List<Status>, validEndStatuses: List<Status>) {
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

                val finalMetricFieldName = if (foundEndStatuses[0].ER_DIGITAL) metricFieldName else metricFieldName.plus("-papir")

                AivenMetrics().registerElapsedTime(finalMetricFieldName, timeDifference)
                SensuMetrics().registerElapsedTime(finalMetricFieldName, timeDifference)
            }
        } catch (e: Exception) {
            logg.error { "Feil ved sending av tid mellom status metrikker: ${e.message}. ${e.stackTrace}" }
        }
    }

    companion object {
        const val TID_FRA_VENTER_GODKJENNING_TIL_GODKJENT = "hm-soknadsbehandling.event.tid_fra_venter_godkjenning_til_godkjent"
        const val TID_FRA_GODKJENT_TIL_JOURNALFORT = "hm-soknadsbehandling.event.tid_fra_godkjent_til_journalfort"
        const val TID_FRA_JOURNALFORT_TIL_VEDTAK = "hm-soknadsbehandling.event.tid_fra_journalfort_til_vedtak"
        const val TID_FRA_VEDTAK_TIL_UTSENDING = "hm-soknadsbehandling.event.tid_fra_vedtak_til_utsending"
    }
}
