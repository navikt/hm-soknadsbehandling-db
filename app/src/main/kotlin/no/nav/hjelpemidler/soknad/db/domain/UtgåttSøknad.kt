package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.TilknyttetSøknad

class UtgåttSøknad(
    override val søknadId: BehovsmeldingId,
    val status: BehovsmeldingStatus,
    val fnrBruker: String,
) : TilknyttetSøknad
