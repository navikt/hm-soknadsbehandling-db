package no.nav.hjelpemidler.behovsmeldingsmodell.sak

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.hjelpemidler.behovsmeldingsmodell.TilknyttetSøknad
import java.time.Instant

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "system",
)
@JsonSubTypes(
    JsonSubTypes.Type(HotsakSak::class, name = Fagsak.System.HOTSAK_NAME),
    JsonSubTypes.Type(InfotrygdSak::class, name = Fagsak.System.INFOTRYGD_NAME),
)
sealed interface Fagsak : TilknyttetSøknad {
    val sakId: Id
    val opprettet: Instant
    val vedtak: Vedtak?
    val system: System

    /**
     * FagsakId i SAF/Joark.
     */
    sealed interface Id : CharSequence

    /**
     * Fagsaksystem i SAF/Joark.
     */
    enum class System {
        /**
         * HJELPEMIDLER i SAF/Joark.
         */
        @JsonAlias("HJELPEMIDLER")
        HOTSAK,

        /**
         * IT01 i SAF/Joark.
         */
        @JsonAlias("IT01")
        INFOTRYGD,
        ;

        companion object {
            const val HOTSAK_NAME = "HOTSAK"
            const val INFOTRYGD_NAME = "INFOTRYGD"
        }
    }
}
