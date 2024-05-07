package no.nav.hjelpemidler.behovsmeldingsmodell

import java.util.UUID

data class Brukerpassbytte(
    val id: UUID,
    var fnr: Fødselsnummer? = null,
    val hjelpemiddel: BrukerpassbytteHjelpemiddel,
    val bytteårsak: BrukerpassbytteÅrsak,
    val byttebegrunnelse: String?,
    val utleveringsmåte: BrukerpassbytteUtleveringsmåte,
    val brukersNavn: String,
    val folkeregistrertAdresse: BrukerpassbytteAdresse,
    val adresse: BrukerpassbytteAdresse?,
    val dato: String,
)

data class BrukerpassbytteHjelpemiddel(
    val artnr: String, // er dette hmsnr?
    val navn: String,
    val kategori: String,
    val kategorinummer: String,
)

enum class BrukerpassbytteÅrsak {
    UTSLITT,
    ØDELAGT,
    ANNEN_ÅRSAK,
}

enum class BrukerpassbytteUtleveringsmåte {
    FOLKEREGISTRERT_ADRESSE,
    OPPGITT_ADRESSE,
}

data class BrukerpassbytteAdresse(
    val adresse: String?,
    val postnr: String?,
    val poststed: String?,
)
