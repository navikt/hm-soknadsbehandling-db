package no.nav.hjelpemidler.behovsmeldingsmodell.v1

import com.fasterxml.jackson.annotation.JsonAlias

sealed interface GeografiskTilknytning

data class Veiadresse(
    val adresse: String,
    val postnummer: String,
    val poststed: String,
) : GeografiskTilknytning {
    override fun toString(): String = "$adresse, $postnummer $poststed"
}

fun lagVeiadresse(adresse: String, postnummer: String, poststed: String): Veiadresse =
    Veiadresse(adresse, postnummer, poststed)

@JvmName("lagVeiadresseOrNull")
fun lagVeiadresse(adresse: String?, postnummer: String?, poststed: String?): Veiadresse? =
    if (adresse.isNullOrBlank() || postnummer.isNullOrBlank() || poststed.isNullOrBlank()) {
        null
    } else {
        lagVeiadresse(adresse, postnummer, poststed)
    }

sealed class GeografiskOmråde : GeografiskTilknytning {
    abstract val nummer: String
    abstract val navn: String

    override fun toString(): String = "$navn ($nummer)"
}

data class Kommune(
    @JsonAlias("kommunenummer")
    override val nummer: String,
    @JsonAlias("kommunenavn")
    override val navn: String,
) : GeografiskOmråde()

data class Bydel(
    @JsonAlias("bydelsnummer")
    override val nummer: String,
    @JsonAlias("bydelsnavn")
    override val navn: String,
) : GeografiskOmråde()

data class Enhet(
    @JsonAlias("enhetsnummer")
    override val nummer: String,
    @JsonAlias("enhetsnavn")
    override val navn: String,
) : GeografiskOmråde()
