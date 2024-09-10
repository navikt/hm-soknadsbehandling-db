package no.nav.hjelpemidler.behovsmeldingsmodell

import com.fasterxml.jackson.annotation.JsonValue

data class Fødselsnummer(private val value: String) {

    init {
        require(value.all { it.isDigit() } && value.length == 11) { "$value er ikke et gyldig fødselsnummer. Må bestå av 11 siffer." }
    }

    @JsonValue
    override fun toString(): String = value
}
