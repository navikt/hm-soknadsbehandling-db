package no.nav.hjelpemidler.behovsmeldingsmodell

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

data class BehovsmeldingEvent(
    @JsonProperty("fodselNrBruker")
    val fnrBruker: String,
    @JsonProperty("fodselNrInnsender")
    val fnrInnsender: String,
    @JsonProperty("soknad")
    val behovsmelding: Behovsmelding,
    val signatur: Signaturtype,
    val kommunenavn: String?,
    val eventId: UUID = UUID.randomUUID(),
    val eventName: String,
    val opprettetDato: LocalDateTime = LocalDateTime.now(),
)
