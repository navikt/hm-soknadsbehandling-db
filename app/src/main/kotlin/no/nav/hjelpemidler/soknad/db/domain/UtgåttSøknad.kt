package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import java.util.UUID

class UtgåttSøknad(
    val søknadId: UUID,
    val status: BehovsmeldingStatus,
    val fnrBruker: String,
)
