package no.nav.hjelpemidler.soknad.db.rapportering

import java.time.LocalDateTime

class Rapporteringsjobber(
    private val jobbScheduler: JobbScheduler,
    private val manglendeOppgaver: ManglendeOppgaver,
    private val manglendeBrukerbekreftelse: ManglendeBrukerbekreftelse,
) {
    fun schedulerJobber() {
        jobbScheduler.schedulerGjentagendeJobb(
            navn = "rapporter_behovsmeldinger_som_mangler_oppgave",
            jobb = { manglendeOppgaver.rapporter() },
            beregnNesteKjøring = { manglendeOppgaver.nesteKjøring() },
        )

        jobbScheduler.schedulerGjentagendeJobb(
            navn = "rapporter_manglende_brukerbekreftelse_til_formidler",
            jobb = { manglendeBrukerbekreftelse.rapporter() },
            beregnNesteKjøring = { manglendeBrukerbekreftelse.nesteKjøring() },
        )

        // TODO: Midlertidig rekjøring etter at nattens kjøring 13.11.2026 feilet. Fjernes etter kjøring.
        jobbScheduler.schedulerEngangsjobb(
            navn = "rapporter_manglende_brukerbekreftelse_til_formidler_rekjoring",
            jobb = { manglendeBrukerbekreftelse.rapporter() },
            tidspunkt = LocalDateTime.of(2026, 11, 13, 10, 15),
        )
    }
}
