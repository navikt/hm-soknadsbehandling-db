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
    fun recordTidFraInnsendtTilVedtak(session: Session, soknadsId: UUID, status: Status) {
        runBlocking {
            launch(Job()) {

                try {
                    // TODO consider extracting for new measurements
                    val validStartStatuses = listOf(
                        Status.GODKJENT,
                        Status.GODKJENT_MED_FULLMAKT
                    )
                    val validEndStatuses = listOf(
                        Status.VEDTAKSRESULTAT_ANNET,
                        Status.VEDTAKSRESULTAT_AVSLÅTT,
                        Status.VEDTAKSRESULTAT_DELVIS_INNVILGET,
                        Status.VEDTAKSRESULTAT_INNVILGET,
                        Status.VEDTAKSRESULTAT_MUNTLIG_INNVILGET
                    )

                    if (status in validEndStatuses)
                        recordTimeElapsedBetweenStatusChange(
                            session,
                            soknadsId,
                            TID_FRA_INNSENDT_TIL_VEDTAK,
                            validStartStatuses,
                            validEndStatuses
                        )
                } catch (e: Exception) {
                    logg.error { "Feilet ved sending av status endring metrikker: ${e.message}. ${e.stackTrace}" }
                }
            }
        }
    }

    fun recordTidFraJournalfortTilVedtak(session: Session, soknadsId: UUID, status: Status) {
        runBlocking {
            launch(Job()) {

                try {
                    // TODO consider extracting for new measurements
                    val validStartStatuses = listOf(
                        Status.ENDELIG_JOURNALFØRT,
                    )
                    val validEndStatuses = listOf(
                        Status.VEDTAKSRESULTAT_ANNET,
                        Status.VEDTAKSRESULTAT_AVSLÅTT,
                        Status.VEDTAKSRESULTAT_DELVIS_INNVILGET,
                        Status.VEDTAKSRESULTAT_INNVILGET,
                        Status.VEDTAKSRESULTAT_MUNTLIG_INNVILGET
                    )

                    if (status in validEndStatuses)
                        recordTimeElapsedBetweenStatusChange(
                            session,
                            soknadsId,
                            TID_FRA_JOURNALFORT_TIL_VEDTAK,
                            validStartStatuses,
                            validEndStatuses
                        )
                } catch (e: Exception) {
                    logg.error { "Feilet ved sending av status endring metrikker: ${e.message}. ${e.stackTrace}" }
                }
            }
        }
    }

    fun recordTidFraVedtakTilUtsending(session: Session, soknadsId: UUID, status: Status) {
        runBlocking {
            launch(Job()) {

                try {
                    // TODO consider extracting for new measurements
                    val validStartStatuses = listOf(
                        Status.VEDTAKSRESULTAT_ANNET,
                        Status.VEDTAKSRESULTAT_AVSLÅTT,
                        Status.VEDTAKSRESULTAT_DELVIS_INNVILGET,
                        Status.VEDTAKSRESULTAT_INNVILGET,
                        Status.VEDTAKSRESULTAT_MUNTLIG_INNVILGET
                    )
                    val validEndStatuses = listOf(
                        Status.UTSENDING_STARTET,
                    )

                    if (status in validEndStatuses)
                        recordTimeElapsedBetweenStatusChange(
                            session,
                            soknadsId,
                            TID_FRA_VEDTAK_TIL_UTSENDING,
                            validStartStatuses,
                            validEndStatuses
                        )
                } catch (e: Exception) {
                    logg.error { "Feilet ved sending av status endring metrikker: ${e.message}. ${e.stackTrace}" }
                }
            }
        }
    }

    private fun recordTimeElapsedBetweenStatusChange(session: Session, soknadsId: UUID, metricFieldName: String, validStartStatuses: List<Status>, validEndStatuses: List<Status>) {

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

        if (foundEndStatuses[0].ER_DIGITAL) {
            AivenMetrics().registerElapsedTime(metricFieldName, timeDifference)
            SensuMetrics().registerElapsedTime(metricFieldName, timeDifference)
        } else {
            AivenMetrics().registerElapsedTime(metricFieldName.plus("-papir"), timeDifference)
            SensuMetrics().registerElapsedTime(metricFieldName.plus("-papir"), timeDifference)
        }
    }

    companion object {
        const val TID_FRA_INNSENDT_TIL_VEDTAK = "hm-soknadsbehandling.event.tid_fra_innsendt_til_vedtak"
        const val TID_FRA_JOURNALFORT_TIL_VEDTAK = "hm-soknadsbehandling.event.tid_fra_journalfort_til_vedtak"
        const val TID_FRA_VEDTAK_TIL_UTSENDING = "hm-soknadsbehandling.event.tid_fra_vedtak_til_utsending"
    }
}
