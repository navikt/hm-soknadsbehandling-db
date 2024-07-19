package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import java.util.UUID

data class PapirSøknadData(
    val fnrBruker: String,
    val soknadId: UUID,
    val status: BehovsmeldingStatus,
    val journalpostid: Int,
    val navnBruker: String,
)
