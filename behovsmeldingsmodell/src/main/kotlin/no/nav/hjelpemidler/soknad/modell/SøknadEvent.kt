package no.nav.hjelpemidler.soknad.modell

import java.time.LocalDateTime
import java.util.UUID

data class SøknadEvent(
    val fodselNrBruker: String,
    val fodselNrInnsender: String,
    val soknad: Behovsmelding,
    val eventId: UUID,
    val eventName: String,
    val signatur: Signaturtype,
    val kommunenavn: String?,
    val opprettetDato: LocalDateTime = LocalDateTime.now(),
)
