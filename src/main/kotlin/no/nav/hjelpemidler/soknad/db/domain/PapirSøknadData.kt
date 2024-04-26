package no.nav.hjelpemidler.soknad.db.domain

import java.util.UUID

data class PapirSøknadData(
    val fnrBruker: String,
    val soknadId: UUID,
    val status: Status,
    val journalpostid: Int,
    val navnBruker: String,
)
