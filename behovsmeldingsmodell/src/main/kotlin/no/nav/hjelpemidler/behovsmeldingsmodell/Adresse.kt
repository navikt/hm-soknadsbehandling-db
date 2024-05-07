package no.nav.hjelpemidler.behovsmeldingsmodell

interface Adresse {
    val adresselinje1: String
    val adresselinje2: String?
    val adresselinje3: String?
    val postnummer: String
    val poststed: String
}
