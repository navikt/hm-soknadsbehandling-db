package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.TilknyttetSøknad
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Innsenderbehovsmelding

class BehovsmeldingSomVenterGodkjenningDto(
    override val søknadId: BehovsmeldingId,
    val status: BehovsmeldingStatus,
    val fnrBruker: String,
) : TilknyttetSøknad

data class BehovsmeldingOgStatus(
    val behovsmelding: Innsenderbehovsmelding,
    val status: BehovsmeldingStatus,
) {
    fun tilBehovsmeldingSomVenterGodkjenningDto() = BehovsmeldingSomVenterGodkjenningDto(
        søknadId = behovsmelding.id,
        status = status,
        fnrBruker = behovsmelding.bruker.fnr.toString(),
    )
}
