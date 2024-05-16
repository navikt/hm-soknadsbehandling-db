package no.nav.hjelpemidler.behovsmeldingsmodell

data class Personnavn(
    val fornavn: String,
    val etternavn: String,
) {
    override fun toString(): String = listOf(fornavn, etternavn).filter(String::isNotBlank).joinToString(" ")
}

fun lagPersonnavn(fornavn: String, etternavn: String): Personnavn =
    Personnavn(fornavn, etternavn)

@JvmName("lagPersonnavnOrNull")
fun lagPersonnavn(fornavn: String?, etternavn: String?): Personnavn? =
    if (fornavn.isNullOrBlank() || etternavn.isNullOrBlank()) {
        null
    } else {
        lagPersonnavn(fornavn, etternavn)
    }

interface HarPersonnavn {
    val navn: Personnavn
}
