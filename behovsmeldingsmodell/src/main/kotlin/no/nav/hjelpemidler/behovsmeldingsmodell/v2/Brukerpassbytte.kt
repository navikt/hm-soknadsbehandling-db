package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import no.nav.hjelpemidler.behovsmeldingsmodell.v1.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Fødselsnummer
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Personnavn
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Veiadresse
import java.time.LocalDate
import java.util.UUID


class Brukerpassbytte(
    id: UUID,
    innsendingsdato: LocalDate,

    val fnr: Fødselsnummer,
    val navn: Personnavn,
    val folkeregistrertAdresse: Veiadresse,
    val annenAdresse: Veiadresse,
    val hjelpemiddel: Hjelpemiddel,
    val bytteårsak: Bytteårsak,
    val byttebegrunnelse: String?,
    val utleveringsmåte: Utleveringsmåte,
    val dato: LocalDate,
) : Behovsmelding(
    id = id,
    type = BehovsmeldingType.BRUKERPASSBYTTE,
    innsendingsdato = innsendingsdato,
    prioritet = Prioritet.NORMAL
) {
    data class Hjelpemiddel(
        val artnr: String, // TODO skal vi standardisere på artnr eller hmsnr?
        val navn: String,
        val kategori: String,
        val iso6: Iso6,
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

}