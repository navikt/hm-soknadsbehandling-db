package no.nav.hjelpemidler.soknad.db.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.store.Transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Timer
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

private val logg = KotlinLogging.logger {}

class Metrics(
    private val transaction: Transaction,
    private val influxDB: InfluxDB = InfluxDB(),
) {
    init {
        Timer("metrics", true).schedule(
            timerTask {
                runBlocking(Dispatchers.IO) {
                    launch {
                        countApplicationsByStatus()
                    }
                }
            },
            midnatt(),
            TimeUnit.DAYS.toMillis(1),
        )
    }

    suspend fun measureElapsedTimeBetweenStatusChanges(søknadId: UUID, status: Status) = withContext(Dispatchers.IO) {
        launch {
            recordForStatus(
                søknadId,
                status,
                TID_FRA_VENTER_GODKJENNING_TIL_GODKJENT,
                listOf(Status.VENTER_GODKJENNING),
                listOf(Status.GODKJENT),
            )
            recordForStatus(
                søknadId,
                status,
                TID_FRA_GODKJENT_TIL_JOURNALFORT,
                listOf(Status.GODKJENT, Status.GODKJENT_MED_FULLMAKT),
                listOf(Status.ENDELIG_JOURNALFØRT),
            )
            recordForStatus(
                søknadId,
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
                ),
            )
            recordForStatus(
                søknadId,
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
                listOf(Status.UTSENDING_STARTET),
            )
        }
    }

    private suspend fun recordForStatus(
        søknadId: UUID,
        status: Status,
        metricFieldName: String,
        validStartStatuses: List<Status>,
        validEndStatuses: List<Status>,
    ) {
        try {
            if (status in validEndStatuses) {
                val result = transaction { søknadStore.hentStatuser(søknadId) }

                // todo -> if multiple statuses converged to a single common status this would not be necessary
                val foundEndStatuses = result.filter { statusRow -> statusRow.STATUS in validEndStatuses }
                if (foundEndStatuses.isEmpty()) return
                val foundStartStatuses = result.filter { statusRow -> statusRow.STATUS in validStartStatuses }
                if (foundStartStatuses.isEmpty()) return

                val earliestEndStatus = foundEndStatuses.minByOrNull { statusRow -> statusRow.CREATED } ?: return
                val earliestStartStatus = foundStartStatuses.minByOrNull { statusRow -> statusRow.CREATED } ?: return

                val timeDifference = earliestEndStatus.CREATED.time - earliestStartStatus.CREATED.time

                val finalMetricFieldName =
                    if (foundEndStatuses[0].ER_DIGITAL) {
                        metricFieldName
                    } else {
                        metricFieldName.plus("-papir")
                    }

                influxDB.registerElapsedTime(finalMetricFieldName, timeDifference)
            }
        } catch (e: Exception) {
            logg.error(e) { "Feil ved sending av tid mellom status-metrikker" }
        }
    }

    private suspend fun countApplicationsByStatus() {
        try {
            val result = transaction { søknadStore.tellStatuser() }

            val metricsToSend = result.associate { statusRow ->
                statusRow.STATUS.toString() to statusRow.COUNT.toInt()
            }

            if (metricsToSend.isNotEmpty()) {
                influxDB.registerStatusCounts(COUNT_OF_SOKNAD_BY_STATUS, metricsToSend)
            }
        } catch (e: Exception) {
            logg.error(e) { "Feil ved sending antall per status metrikker." }
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

private fun midnatt(): Date = LocalDate.now().plusDays(1).atStartOfDay().toDate()
private fun LocalDateTime.toDate(): Date = Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
