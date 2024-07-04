package no.nav.hjelpemidler.soknad.db

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.engine.apache.Apache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.http.slack.slack
import no.nav.hjelpemidler.http.slack.slackIconEmoji
import no.nav.hjelpemidler.soknad.db.db.Transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Timer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

private val logg = KotlinLogging.logger {}

private const val MINIMUM_DAGER = 2

class Oppgaveinspektør(
    private val transaction: Transaction,
    startTime: Date = midnatt(),
) {
    private val slack = slack(engine = Apache.create())

    init {
        Timer("SaksflytInspektør", true).schedule(
            timerTask {
                runBlocking(Dispatchers.IO) {
                    launch {
                        rapporterBehovsmeldingerSomManglerOppgave()
                    }
                }
            },
            startTime,
            TimeUnit.DAYS.toMillis(1),
        )
    }

    /**
     * Dette er saker som har stoppet opp pga. manglende verdier i innsendingen.
     * Feilen skal ha blitt rettet, og formidler har blitt informert om at de må sende inn på nytt.
     * Det må vurderes konsekvens av å slette disse fra systemet dersom vi evt. skal gjøre det.
     * F.eks.: Er sakene synlig for bruker?
     */
    private val ignoreList = listOf("bdd3b385-9add-4317-bf82-d8df0e6974e0", "ae15f3a2-f74b-4465-a636-7f527c98d0a0")

    private suspend fun rapporterBehovsmeldingerSomManglerOppgave() {
        logg.info { "Sjekk om det finnes behovsmeldinger som mangler oppgave..." }
        try {
            val godkjenteBehovsmeldingerUtenOppgave = transaction {
                søknadStore
                    .hentGodkjenteBehovsmeldingerUtenOppgaveEldreEnn(MINIMUM_DAGER)
                    .filter { it !in ignoreList }
            }

            if (godkjenteBehovsmeldingerUtenOppgave.isEmpty()) {
                logg.info { "Fant ingen behovsmeldinger som mangler oppgave." }
                return
            }

            val message = """
                Det finnes ${godkjenteBehovsmeldingerUtenOppgave.size} godkjente søknader uten oppgave som er eldre enn $MINIMUM_DAGER dager.
                Undersøk hvorfor disse har stoppet opp i systemet.
                søknadID-er (max 10): ${godkjenteBehovsmeldingerUtenOppgave.take(10).joinToString()}
            """.trimIndent()

            if (Environment.current.tier.isProd) {
                slack.sendMessage(
                    username = "hm-soknadsbehandling-db",
                    icon = slackIconEmoji(":this-is-fine-fire:"),
                    channel = "#digihot-alerts",
                    message = message,
                )
            }

            logg.error { message }
        } catch (e: Exception) {
            logg.error(e) { "Feil under rapportering av godkjente søknader som mangler oppgave." }
        }
    }
}

private fun midnatt(): Date = LocalDate.now().plusDays(1).atStartOfDay().toDate()
private fun LocalDateTime.toDate(): Date = Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
