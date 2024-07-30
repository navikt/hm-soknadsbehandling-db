package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.Fødselsnummer
import java.time.LocalDate
import java.util.UUID

// Kan vi ha en minimal Behovsmelding-klasse som er den som legges på Kafka og som
// dekker behovet til de fleste apper (som ikke har behov for hele behovsmeldingen)?
open class Behovsmelding(
    val id: UUID,
    val type: BehovsmeldingType,
    val innsendingsdato: LocalDate,
    val prioritet: Prioritet,
    val hjmBrukersFnr: Fødselsnummer,
    val innsendersFnr: Fødselsnummer,
    val skjemaversjon: Int = 2,
)
