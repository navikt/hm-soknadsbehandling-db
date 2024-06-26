package no.nav.hjelpemidler.behovsmeldingsmodell.v2

interface Visbar {
    val visning: Visning
}

enum class Visning {
    NORMAL, LISTE, INLINE, MULTI_INLINE
}

private interface Enkeltekst<T> {
    val verdi: T
}

private interface Listetekst<T> {
    val verdier: List<Enkeltverdi<T>>
}

/**
 * verdi vil bli brukt som del av translationkey.
 * annenFritekst vil vises i stedet for verdi dersom annenFritekst er satt.
 * Brukes feks når verdi==ANNET og begrunner må beskrive med fritekst.
 */
data class Enkeltverdi<T>(
    override val verdi: T,
    val annenFritekst: String? = null,
): Visbar, Enkeltekst<T> {
    override val visning: Visning = Visning.NORMAL
}

data class Fritekst(
    override val verdi: String
): Visbar, Enkeltekst<String> {
    override val visning: Visning = Visning.NORMAL
}

data class Listeverdi<T>(
    override val verdier: List<Enkeltverdi<T>>
): Visbar, Listetekst<T> {
    override val visning: Visning = Visning.LISTE
}

data class Inlineverdi<T>(
    override val verdi: T
): Visbar, Enkeltekst<T> {
    override val visning = Visning.INLINE
}

// TODO Hvordan definere multi-inline verdier på en god måte?
// E.g. kroppsmål