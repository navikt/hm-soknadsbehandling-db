package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadId
import no.nav.hjelpemidler.soknad.db.soknad.TilknyttetSøknad
import java.util.UUID

/**
 * @see [no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingResponse]
 */
data class SøknadData(
    val fnrBruker: String,
    val navnBruker: String,
    val fnrInnsender: String?,
    val soknadId: UUID,
    val soknad: JsonNode,
    val status: BehovsmeldingStatus,
    val kommunenavn: String?,
    val er_digital: Boolean,
    val soknadGjelder: String?,
) : TilknyttetSøknad {
    override val søknadId: SøknadId @JsonIgnore get() = soknadId
}
