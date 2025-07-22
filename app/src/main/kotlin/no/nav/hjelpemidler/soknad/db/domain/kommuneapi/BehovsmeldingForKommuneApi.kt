package no.nav.hjelpemidler.soknad.db.domain.kommuneapi

import no.nav.hjelpemidler.soknad.db.domain.kommuneapi.v2.Innsenderbehovsmelding
import java.time.LocalDateTime
import java.util.UUID

data class BehovsmeldingForKommuneApi(
    val fnrBruker: String,
    val navnBruker: String,
    val fnrInnsender: String?,
    val behovsmeldingId: UUID,
    val behovsmelding: Innsenderbehovsmelding,
    val behovsmeldingGjelder: String?,
    val opprettet: LocalDateTime,
)
