package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.Fødselsnummer
import java.time.LocalDate
import java.util.UUID

interface BehovsmeldingBase {
    val id: UUID
    val type: BehovsmeldingType
    val innsendingsdato: LocalDate
    val prioritet: Prioritet
    val hjmBrukersFnr: Fødselsnummer
    val innsendersFnr: Fødselsnummer
    val skjemaversjon: Int
}

/*
 Minimal Behovsmelding-klasse som er den som legges på Kafka og som
 dekker behovet til de apper som ikke har behov for å vise frem hele behovsmeldingen.
 */
// TODO: Synes vi dette er nyttig, eller vil vi heller ha feks. supersmå events (eventname+søknadId)
// og flere kall til soknadsbehandling-db?
data class Behovsmelding(
    override val id: UUID,
    override val type: BehovsmeldingType,
    override val innsendingsdato: LocalDate,
    override val prioritet: Prioritet,
    override val hjmBrukersFnr: Fødselsnummer,
    override val innsendersFnr: Fødselsnummer,
    override val skjemaversjon: Int,
) : BehovsmeldingBase
