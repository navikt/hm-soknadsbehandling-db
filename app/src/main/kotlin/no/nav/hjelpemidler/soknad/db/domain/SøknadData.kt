package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadId
import no.nav.hjelpemidler.behovsmeldingsmodell.TilknyttetSøknad

/**
 * @see [no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingResponse]
 */
@Deprecated("Bruk no.nav.hjelpemidler.soknad.db.domain.Søknad")
data class SøknadData(
    val fnrBruker: String,
    val navnBruker: String,
    val fnrInnsender: String?,
    val soknadId: SøknadId,
    val soknad: JsonNode,
    val status: BehovsmeldingStatus,
    val kommunenavn: String?,
    val er_digital: Boolean,
    val soknadGjelder: String?,
) : TilknyttetSøknad {
    override val søknadId: SøknadId @JsonIgnore get() = soknadId

    constructor(søknad: Søknad) : this(
        fnrBruker = søknad.fnrBruker,
        navnBruker = søknad.navnBruker,
        fnrInnsender = søknad.fnrInnsender,
        soknadId = søknad.søknadId,
        soknad = søknad.data,
        status = søknad.status,
        kommunenavn = søknad.kommunenavn,
        er_digital = søknad.digital,
        soknadGjelder = søknad.søknadGjelder,
    )
}
