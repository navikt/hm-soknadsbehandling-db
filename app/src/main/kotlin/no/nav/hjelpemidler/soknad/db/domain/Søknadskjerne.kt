package no.nav.hjelpemidler.soknad.db.domain

import java.time.Instant
import java.util.UUID

data class Søknadskjerne(
    val søknadId: UUID,
    val fnrBruker: String,
    val fnrInnsender: String,
    val datoOpprettet: Instant,
    val behovsmeldingstype: String,
    val søknadstype: String? = null, // bare for infotrygd?
)
