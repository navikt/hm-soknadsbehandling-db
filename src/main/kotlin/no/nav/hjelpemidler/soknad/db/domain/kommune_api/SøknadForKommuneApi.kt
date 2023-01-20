package no.nav.hjelpemidler.soknad.db.domain.kommune_api

import java.time.LocalDateTime
import java.util.UUID

data class SøknadForKommuneApi(
    val fnrBruker: String,
    val navnBruker: String,
    val fnrInnsender: String?,
    val soknadId: UUID,
    val soknad: Behovsmelding,
    val soknadGjelder: String?,
    val opprettet: LocalDateTime,
)
