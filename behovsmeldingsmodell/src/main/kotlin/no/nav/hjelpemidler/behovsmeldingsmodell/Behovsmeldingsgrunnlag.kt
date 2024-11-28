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
        override val søknadId: BehovsmeldingId,
        override val status: BehovsmeldingStatus,
        override val fnrBruker: String,
        override val navnBruker: String,
        val fnrInnsender: String?,
        @JsonAlias("soknad")
        val behovsmelding: Map<String, Any?>, // JsonNode
        @JsonAlias("soknadGjelder")
        val behovsmeldingGjelder: String?,
        /**
         * I overgangsfase vil vi kunne motta både behovsmelding og behvosmeldingV2.
         * Løs typing her, slik at vi slipper å oppdatere hm-soknadsbehandling hver gang
         * behovsmeldingsmodellen oppdaterer seg. hm-soknadsbehandling skal uansett bare videre sende data.
         * Typing av data i hm-soknad-api og hm-soknadsbehandling-db.
         */
        val behovsmeldingV2: Map<String, Any?>? = null,
    ) : Behovsmeldingsgrunnlag {
        override val kilde: Kilde = Kilde.DIGITAL
        override val digital: Boolean = true
    }

    data class Papir(
        override val søknadId: BehovsmeldingId,
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
