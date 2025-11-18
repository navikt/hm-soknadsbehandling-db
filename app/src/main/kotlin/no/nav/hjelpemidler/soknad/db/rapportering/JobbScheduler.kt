package no.nav.hjelpemidler.soknad.db.rapportering

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }

class JobbScheduler(
    private val scheduler: ScheduledExecutorService,
    private val leaderElection: LeaderElection,
) {

    fun schedulerGjentagendeJobb(
        navn: String,
        jobb: suspend CoroutineScope.() -> Unit,
        beregnNesteKjøring: () -> LocalDateTime,
    ) {
        val task = Runnable {
            runBlocking { kjørJobb(navn, jobb) }

            // Reschedule neste kjøring til ønsket tidspunkt.
            schedulerGjentagendeJobb(navn, jobb, beregnNesteKjøring)
        }

        val nesteKjøring = beregnNesteKjøring()
        val forsinkelseTilNesteKjøring = Duration.between(LocalDateTime.now(), nesteKjøring).toMillis()

        require(forsinkelseTilNesteKjøring > 0) { "Kan ikke ha negativ forsinkelse til neste kjøring. Navn=$navn, forsinkelse=$forsinkelseTilNesteKjøring, nesteKjøring=$nesteKjøring" }

        log.info { "Schedulerer neste kjøring av $navn til $nesteKjøring (delay=$forsinkelseTilNesteKjøring ms)" }
        scheduler.schedule(task, forsinkelseTilNesteKjøring, TimeUnit.MILLISECONDS)
    }

    suspend fun CoroutineScope.kjørJobb(navn: String, jobb: suspend CoroutineScope.() -> Unit) {
        if (!leaderElection.erLeder()) {
            log.info { "Hopper over jobb $navn fordi denne instansen ikke er leder." }
            return
        }

        log.info { "Starter jobb $navn..." }
        try {
            jobb()
            log.info { "Jobb $navn fullført." }
        } catch (e: Exception) {
            log.error(e) { "Jobb $navn feilet." }
        }
    }
}
