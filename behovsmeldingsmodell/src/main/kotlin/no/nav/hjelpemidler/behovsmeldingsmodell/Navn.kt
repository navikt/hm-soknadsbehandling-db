package no.nav.hjelpemidler.behovsmeldingsmodell

data class Navn(
    val fornavn: String,
    val etternavn: String,
) {
    override fun toString(): String = "$fornavn $etternavn"
}
