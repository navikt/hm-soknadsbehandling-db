package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

internal data class SoknadData(
    val fnrBruker: String,
    val navnBruker: String,
    val fnrInnsender: String?,
    val soknadId: UUID,
    val soknad: JsonNode,
    val status: Status,
    val kommunenavn: String?,
    val er_digital: Boolean,
    val soknadGjelder: String?,
)
