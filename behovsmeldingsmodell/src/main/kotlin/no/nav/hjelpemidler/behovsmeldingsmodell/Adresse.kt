package no.nav.hjelpemidler.behovsmeldingsmodell

interface Adresse {
    val adresse: String
    val adresselinje1: String get() = adresse
    val adresselinje2: String? get() = null
    val adresselinje3: String? get() = null
    val postnummer: String
    val poststed: String
}
