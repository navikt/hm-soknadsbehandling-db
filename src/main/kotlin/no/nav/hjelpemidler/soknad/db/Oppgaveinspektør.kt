package no.nav.hjelpemidler.soknad.db

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KLogger
import mu.KotlinLogging
import no.nav.hjelpemidler.http.slack.slack
import no.nav.hjelpemidler.http.slack.slackIconEmoji
import no.nav.hjelpemidler.soknad.db.db.SøknadStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Timer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

private val logger: KLogger = KotlinLogging.logger {}

private val MINIMUM_DAGER = 2

internal class Oppgaveinspektør(
    private val søknadStore: SøknadStore,
    startTime: Date = midnatt(),
) {

    init {
        Timer("SaksflytInspektør", true).schedule(
            timerTask {
                runBlocking {
                    launch {
                        rapporterBehovsmeldingerSomManglerOppgave(søknadStore)
                    }
                }
            },
            startTime,
            TimeUnit.DAYS.toMillis(1),
        )
    }

    /*
    Dette er saker som har stoppet opp pga manglende verdier i innsendingen.
    Feilen skal ha blitt rettet, og formidler har blitt informert om at de må sende inn på nytt.
    Det må vurderes konsekvens av å slette disse fra systemet dersom vi evt. skal gjøre det.
    F.eks.: Er sakene synlig for bruker?
     */
    private val ignoreList = listOf("bdd3b385-9add-4317-bf82-d8df0e6974e0", "ae15f3a2-f74b-4465-a636-7f527c98d0a0")

    private suspend fun rapporterBehovsmeldingerSomManglerOppgave(søknadStore: SøknadStore) {
        logger.info { "Sjekk om det finnes behovsmeldinger som mangler oppgave..." }
        try {
            val godkjenteBehovsmeldingerUtenOppgave = søknadStore
                .hentGodkjenteBehovsmeldingerUtenOppgaveEldreEnn(MINIMUM_DAGER)
                .filter { it !in ignoreList }

            if (godkjenteBehovsmeldingerUtenOppgave.isEmpty()) {
                logger.info { "Fant ingen behovsmeldinger som mangler oppgave." }
                return
            }

            val message = """
                    Det finnes ${godkjenteBehovsmeldingerUtenOppgave.size} godkjente søknader uten oppgave som er eldre enn $MINIMUM_DAGER dager.
                    Undersøk hvorfor disse har stoppet opp i systemet.
                    søknads-IDer (max 10): ${godkjenteBehovsmeldingerUtenOppgave.take(10).joinToString()}
            """.trimIndent()

            if (Configuration.application.profile == Profile.PROD) {
                slack().sendMessage(
                    username = "hm-soknadsbehandling-db",
                    icon = slackIconEmoji(":this-is-fine-fire:"),
                    channel = "#digihot-alerts",
                    message = message,
                )
            }

            logger.error { message }
        } catch (e: Exception) {
            logger.error(e) { "Feil under rapportering av godkjente søknader som mangler oppgave." }
        }
    }
}

private fun midnatt() = LocalDate.now().plusDays(1).atStartOfDay().toDate()

private fun LocalDateTime.toDate(): Date {
    return Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
}
