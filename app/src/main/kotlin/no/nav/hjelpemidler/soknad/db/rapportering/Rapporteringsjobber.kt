package no.nav.hjelpemidler.soknad.db.rapportering

import no.nav.hjelpemidler.soknad.db.rapportering.epost.ContentType
import no.nav.hjelpemidler.soknad.db.rapportering.epost.EPOST_DIGIHOT
import no.nav.hjelpemidler.soknad.db.rapportering.epost.GraphEpost
import java.time.LocalDateTime

class Rapporteringsjobber(
    private val jobbScheduler: JobbScheduler,
    private val manglendeOppgaver: ManglendeOppgaver,
    private val manglendeBrukerbekreftelse: ManglendeBrukerbekreftelse,
    private val epost: GraphEpost,
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

        jobbScheduler.schedulerEngangsjobb(
            navn = "test_epostvarsling",
            jobb = { epost.send(
                avsender = EPOST_DIGIHOT,
                mottaker = "ole.steinar.lillestol.skrede@nav.no",
                tittel = "TEST",
                innholdstype = ContentType.TEXT,
                innhold = """
                Hei!
                
                Dette er en test.
            """.trimIndent(),
                lagreIUtboks = true,
            ) },
            beregnNesteKjøring = { LocalDateTime.now().plusMinutes(2) },
        )
    }
}
