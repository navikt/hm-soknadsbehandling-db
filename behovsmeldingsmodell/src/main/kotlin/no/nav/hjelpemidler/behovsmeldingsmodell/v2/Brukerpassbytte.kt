package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.Fødselsnummer
import no.nav.hjelpemidler.behovsmeldingsmodell.Personnavn
import no.nav.hjelpemidler.behovsmeldingsmodell.Prioritet
import no.nav.hjelpemidler.behovsmeldingsmodell.Veiadresse
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Brukerpassbytte
import java.time.LocalDate
import java.util.UUID

data class Brukerpassbytte(
    val navn: Personnavn,
    val folkeregistrertAdresse: Veiadresse,
    val annenUtleveringsadresse: Veiadresse?,
    val hjelpemiddel: Hjelpemiddel,
    val bytteårsak: Brukerpassbytte.Bytteårsak,
    val byttebegrunnelse: String?,
    val utleveringsmåte: Brukerpassbytte.Utleveringsmåte,

    override val id: UUID,
    override val type: BehovsmeldingType = BehovsmeldingType.BRUKERPASSBYTTE,
    override val skjemaversjon: Int = 2,
    override val innsendingsdato: LocalDate,
    override val hjmBrukersFnr: Fødselsnummer,
    override val innsendersFnr: Fødselsnummer = hjmBrukersFnr,
    override val prioritet: Prioritet = Prioritet.NORMAL,
) : BehovsmeldingBase {
    data class Hjelpemiddel(
        val hmsArtNr: String,
        val artikkelnavn: String,
        val iso6Tittel: String,
        val iso6: Iso6,
    )
}
