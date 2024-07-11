package no.nav.hjelpemidler.soknad.db.domain

import java.util.UUID

data class BrukerpassbytteData(
    val id: UUID,
    val hjelpemiddel: BytteHjelpemiddel,
    val bytteårsak: BrukerpassBytteårsak,
    val byttebegrunnelse: String? = null,
    val utleveringsmåte: BytteUtleveringsmåte,
    val brukersNavn: String,
    val folkeregistrertAdresse: BytteAdresse?,
    val adresse: BytteAdresse?,
    val dato: String,
)

data class BytteHjelpemiddel(
    val artnr: String,
    val navn: String,
    val kategori: String,
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
