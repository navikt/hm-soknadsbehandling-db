package no.nav.hjelpemidler.soknad.db.rapportering

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
    }
}
