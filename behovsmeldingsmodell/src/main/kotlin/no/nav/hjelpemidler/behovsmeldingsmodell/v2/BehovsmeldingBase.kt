package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.Fødselsnummer
import no.nav.hjelpemidler.behovsmeldingsmodell.Prioritet
import java.time.LocalDate
import java.util.UUID

interface BehovsmeldingBase {
    val id: UUID
    val type: BehovsmeldingType
    val innsendingsdato: LocalDate
    val prioritet: Prioritet
    val hjmBrukersFnr: Fødselsnummer
    val skjemaversjon: Int
}
