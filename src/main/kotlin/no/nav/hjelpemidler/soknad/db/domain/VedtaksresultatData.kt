package no.nav.hjelpemidler.soknad.db.domain

import java.time.LocalDate
import java.util.UUID

internal data class VedtaksresultatData(
    val søknadId: UUID,
    val fnrBruker: String,
    val trygdekontorNr: String?,
    val saksblokk: String?,
    val saksnr: String?,
    val vedtaksresultat: String? = null,
    val vedtaksdato: LocalDate? = null,
) {

    companion object {
        fun getTrygdekontorNrFromFagsakId(fagsakId: String): String {
            return fagsakId.take(4)
        }

        fun getSaksblokkFromFagsakId(fagsakId: String): String {
            val saksblokkOgSaksnummer = fagsakId.takeLast(3)
            return saksblokkOgSaksnummer.first().toString()
        }

        fun getSaksnrFromFagsakId(fagsakId: String): String {
            val saksblokkOgSaksnummer = fagsakId.takeLast(3)
            return saksblokkOgSaksnummer.takeLast(2)
        }
    }
}
