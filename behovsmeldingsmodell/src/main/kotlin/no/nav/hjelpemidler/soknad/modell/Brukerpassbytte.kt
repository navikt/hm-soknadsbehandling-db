package no.nav.hjelpemidler.soknad.modell

import java.util.UUID

data class BrukerpassBytteDTO(
    val id: UUID,
    var fnr: String? = null,
    val hjelpemiddel: BytteHjelpemiddel,
    val bytteårsak: BrukerpassBytteårsak,
    val byttebegrunnelse: String?,
    val utleveringsmåte: BytteUtleveringsmåte,
    val brukersNavn: String,
    val folkeregistrertAdresse: BytteAdresse,
    val adresse: BytteAdresse?,
    val dato: String,
)

data class BytteHjelpemiddel(
    val artnr: String,
    val navn: String,
    val kategori: String,
    val kategorinummer: String,
)

enum class BrukerpassBytteårsak {
    UTSLITT,
    ØDELAGT,
    ANNEN_ÅRSAK,
}

enum class BytteUtleveringsmåte {
    FOLKEREGISTRERT_ADRESSE,
    OPPGITT_ADRESSE,
}

data class BytteAdresse(
    val adresse: String?,
    val postnr: String?,
    val poststed: String?,
)
