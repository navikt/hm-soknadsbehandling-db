package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadId
import no.nav.hjelpemidler.behovsmeldingsmodell.TilknyttetSøknad

data class PapirSøknadData(
    override val søknadId: SøknadId,
    @JsonAlias("journalpostid")
    val journalpostId: String,
    val status: BehovsmeldingStatus,
    val fnrBruker: String,
    val navnBruker: String,
) : TilknyttetSøknad
