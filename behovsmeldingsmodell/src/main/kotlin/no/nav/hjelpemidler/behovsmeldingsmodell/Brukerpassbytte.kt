package no.nav.hjelpemidler.behovsmeldingsmodell

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.util.UUID

data class Brukerpassbytte(
    val id: UUID,
    var fnr: Fødselsnummer? = null,
    val hjelpemiddel: Hjelpemiddel,
    val bytteårsak: Bytteårsak,
    val byttebegrunnelse: String?,
    val utleveringsmåte: Utleveringsmåte,
    val brukersNavn: String,
    val folkeregistrertAdresse: Adresse,
    val adresse: Adresse?,
    val dato: LocalDate,
) {
    data class Hjelpemiddel(
        val artnr: String, // er dette hmsnr?
        val navn: String,
        val kategori: String,
        val kategorinummer: String, // hva slags nummer er dette, isokode?
    )

    enum class Bytteårsak {
        UTSLITT,
        ØDELAGT,
        ANNEN_ÅRSAK,
    }

    enum class Utleveringsmåte {
        FOLKEREGISTRERT_ADRESSE,
        OPPGITT_ADRESSE,
    }

    data class Adresse(
        val adresse: String?,
        @JsonProperty("postnr")
        val postnummer: String?,
        val poststed: String?,
    )
}
