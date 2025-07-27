package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.Prioritet
import no.nav.hjelpemidler.domain.geografi.Veiadresse
import no.nav.hjelpemidler.domain.person.Fødselsnummer
import no.nav.hjelpemidler.domain.person.Personnavn
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Brukerpassbytte(
    val navn: Personnavn,
    val folkeregistrertAdresse: Veiadresse,
    val annenUtleveringsadresse: Veiadresse?,
    val hjelpemiddel: Hjelpemiddel,
    val bytteårsak: Bytteårsak,
    val byttebegrunnelse: String?,
    val utleveringsmåte: Utleveringsmåte,

    override val id: UUID,
    override val type: BehovsmeldingType = BehovsmeldingType.BRUKERPASSBYTTE,
    override val skjemaversjon: Int = 2,
    override val innsendingsdato: LocalDate,
    override val innsendingstidspunkt: Instant? = null,
    override val hjmBrukersFnr: Fødselsnummer,
    override val prioritet: Prioritet = Prioritet.NORMAL,
) : BehovsmeldingBase {
    data class Hjelpemiddel(
        val hmsArtNr: String,
        val artikkelnavn: String,
        val iso6Tittel: String,
        val iso6: Iso6?,
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
