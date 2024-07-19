package no.nav.hjelpemidler.soknad.db.sak

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "system",
)
@JsonSubTypes(
    JsonSubTypes.Type(Vedtaksresultat.Hotsak::class, name = Fagsak.System.HOTSAK_NAME),
    JsonSubTypes.Type(Vedtaksresultat.Infotrygd::class, name = Fagsak.System.INFOTRYGD_NAME),
)
sealed interface Vedtaksresultat {
    val vedtaksresultat: String
    val vedtaksdato: LocalDate
    val system: Fagsak.System

    data class Hotsak(
        override val vedtaksresultat: String,
        override val vedtaksdato: LocalDate,
    ) : Vedtaksresultat {
        override val system: Fagsak.System = Fagsak.System.HOTSAK
    }

    data class Infotrygd(
        override val vedtaksresultat: String,
        override val vedtaksdato: LocalDate,
        val søknadstype: String,
    ) : Vedtaksresultat {
        override val system: Fagsak.System = Fagsak.System.INFOTRYGD
    }
}
