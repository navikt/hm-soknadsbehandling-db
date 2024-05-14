package no.nav.hjelpemidler.behovsmeldingsmodell

data class Personnavn(
    val fornavn: String,
    val etternavn: String,
) {
    override fun toString(): String = listOf(fornavn, etternavn).filter(String::isNotBlank).joinToString(" ")
}
