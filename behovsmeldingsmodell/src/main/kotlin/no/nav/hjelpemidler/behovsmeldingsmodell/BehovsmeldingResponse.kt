package no.nav.hjelpemidler.behovsmeldingsmodell

import com.fasterxml.jackson.annotation.JsonAlias
import java.util.UUID

data class BehovsmeldingResponse(
    @JsonAlias("fodselNrBruker")
    val fnrBruker: String,
    val navnBruker: String,
    @JsonAlias("fodselNrInnsender")
    val fnrInnsender: String,
    @JsonAlias("soknadId")
    val behovsmeldingId: UUID,
    @JsonAlias("soknad")
    val behovsmelding: Behovsmelding,
    val status: BehovsmeldingStatus,
    val kommunenavn: String?,
    @JsonAlias("er_digital")
    val erDigital: Boolean,
    @JsonAlias("soknadGjelder")
    val behovsmeldingGjelder: String?,
)
