package no.nav.hjelpemidler.behovsmeldingsmodell

data class Statusendring(
    val status: BehovsmeldingStatus,
    val valgteÅrsaker: Set<String>?,
    val begrunnelse: String?,
)
