package no.nav.hjelpemidler.behovsmeldingsmodell.sak

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

data class HotsakSakId @JsonCreator constructor(@JsonValue val value: String) : Fagsak.Id, CharSequence by value {
    init {
        require(erGyldig(value)) {
            "'$value' er ikke en gyldig HotsakSakId"
        }
    }

    override fun toString(): String = value

    companion object {
        private val regex = Regex("^[0-9]*$")

        fun erGyldig(value: String) = value matches regex
    }
}

/**
 * NB! Saker fra Infotrygd kan ikke entydig identifiseres med denne ID-en.
 */
data class InfotrygdSakId @JsonCreator constructor(@JsonValue val value: String) : Fagsak.Id, CharSequence by value {
    init {
        require(erGyldig(value)) {
            "'$value' er ikke en gyldig InfotrygdSakId"
        }
    }

    @JsonCreator
    constructor(
        @JsonProperty("trygdekontornummer")
        trygdekontornummer: String,
        @JsonProperty("saksblokk")
        saksblokk: String,
        @JsonProperty("saksnummer")
        saksnummer: String,
    ) : this("$trygdekontornummer$saksblokk$saksnummer")

    val trygdekontornummer: String @JsonIgnore get() = value.take(4)
    val saksblokk: String @JsonIgnore get() = value.takeLast(3).take(1)
    val saksnummer: String @JsonIgnore get() = value.takeLast(2)

    override fun toString(): String = value

    companion object {
        private val regex = Regex("^[0-9]{4}[A-Z][0-9]{2}$")

        fun erGyldig(value: String) = value matches regex
    }
}
