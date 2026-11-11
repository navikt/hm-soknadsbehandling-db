package no.nav.hjelpemidler.soknad.db.rolle

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType

data class RollerResultat(
    val tillatelser: Tillatelser,
)

data class Tillatelser(
    val behovsmeldingstyper: Set<BehovsmeldingType> = emptySet(),
)
