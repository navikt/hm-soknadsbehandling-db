package no.nav.hjelpemidler.behovsmeldingsmodell.v1

import com.fasterxml.jackson.annotation.JsonValue

data class Fødselsnummer(private val value: String) {
    @JsonValue
    override fun toString(): String = value
}
