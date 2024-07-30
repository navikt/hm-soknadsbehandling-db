package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.Fødselsnummer
import no.nav.hjelpemidler.behovsmeldingsmodell.Personnavn
import no.nav.hjelpemidler.behovsmeldingsmodell.Veiadresse
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Brukerpassbytte // TODO flytt ut felles enums
import java.time.LocalDate
import java.util.UUID

class Brukerpassbytte(
    id: UUID,
    innsendingsdato: LocalDate,
    hjmBrukersFnr: Fødselsnummer,
    innsendersFnr: Fødselsnummer,

    val navn: Personnavn,
    val folkeregistrertAdresse: Veiadresse,
    val annenUtleveringsadresse: Veiadresse?,
    val hjelpemiddel: Hjelpemiddel,
    val bytteårsak: Brukerpassbytte.Bytteårsak,
    val byttebegrunnelse: String?,
    val utleveringsmåte: Brukerpassbytte.Utleveringsmåte,
) : Behovsmelding(
    id = id,
    type = BehovsmeldingType.BRUKERPASSBYTTE,
    innsendingsdato = innsendingsdato,
    hjmBrukersFnr = hjmBrukersFnr,
    innsendersFnr = innsendersFnr,
    prioritet = Prioritet.NORMAL,
) {
    data class Hjelpemiddel(
        val artnr: String, // TODO skal vi standardisere på artnr eller hmsnr?
        val navn: String,
        val kategori: String, // rename til iso6Navn?
        val iso6: Iso6,
    )
}
