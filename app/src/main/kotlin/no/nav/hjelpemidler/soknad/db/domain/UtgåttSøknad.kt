package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadId

class UtgåttSøknad(
    val søknadId: SøknadId,
    val status: BehovsmeldingStatus,
    val fnrBruker: String,
)
