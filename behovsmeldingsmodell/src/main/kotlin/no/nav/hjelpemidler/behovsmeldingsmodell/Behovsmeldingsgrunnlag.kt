package no.nav.hjelpemidler.behovsmeldingsmodell

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Sakstilknytning

/**
 * Grunnlag for lagring av behovsmelding.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "kilde",
)
@JsonSubTypes(
    JsonSubTypes.Type(Behovsmeldingsgrunnlag.Digital::class, name = "DIGITAL"),
    JsonSubTypes.Type(Behovsmeldingsgrunnlag.Papir::class, name = "PAPIR"),
)
sealed interface Behovsmeldingsgrunnlag : TilknyttetSøknad {
    val status: BehovsmeldingStatus
    val fnrBruker: String
    val navnBruker: String
    val kilde: Kilde

    @get:JsonAlias("er_digital")
    val digital: Boolean

    data class Digital(
        override val søknadId: SøknadId,
        override val status: BehovsmeldingStatus,
        override val fnrBruker: String,
        override val navnBruker: String,
        val fnrInnsender: String?,
        val kommunenavn: String?,
        @JsonAlias("soknad")
        val behovsmelding: Map<String, Any?>, // JsonNode
        @JsonAlias("soknadGjelder")
        val behovsmeldingGjelder: String?,
    ) : Behovsmeldingsgrunnlag {
        override val kilde: Kilde = Kilde.DIGITAL
        override val digital: Boolean = true
    }

    data class Papir(
        override val søknadId: SøknadId,
        override val status: BehovsmeldingStatus,
        override val fnrBruker: String,
        override val navnBruker: String,
        @JsonAlias("journalpostid")
        val journalpostId: String,
        val sakstilknytning: Sakstilknytning.Infotrygd? = null,
    ) : Behovsmeldingsgrunnlag {
        override val kilde: Kilde = Kilde.PAPIR
        override val digital: Boolean = false
    }

    enum class Kilde {
        DIGITAL,
        PAPIR,
    }
}
