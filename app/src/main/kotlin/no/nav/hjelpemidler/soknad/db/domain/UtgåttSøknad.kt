package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadId
import no.nav.hjelpemidler.behovsmeldingsmodell.TilknyttetSøknad

class UtgåttSøknad(
    override val søknadId: SøknadId,
    val status: BehovsmeldingStatus,
    val fnrBruker: String,
) : TilknyttetSøknad
