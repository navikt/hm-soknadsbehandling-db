package no.nav.hjelpemidler.behovsmeldingsmodell

data class Organisasjon(
    val orgnr: String,
    val navn: String,
    val orgform: String = "",
    val overordnetOrgnr: String? = null,
    val næringskoder: List<Næringskode> = emptyList(),
    val kommunenummer: String? = null,
) {
    data class Næringskode(
        val kode: String,
        val beskrivelse: String = "",
    )
}
