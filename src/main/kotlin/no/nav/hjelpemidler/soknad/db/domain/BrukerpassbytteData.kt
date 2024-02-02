package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

internal data class BrukerpassbytteData(
    val id: UUID,
    val fnr: String,
    val brukerpassbytte: JsonNode,
    val status: Status,
    val soknadGjelder: String,
)
