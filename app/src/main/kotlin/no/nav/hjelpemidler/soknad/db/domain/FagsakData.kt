package no.nav.hjelpemidler.soknad.db.domain

import java.util.UUID

internal data class FagsakData(
    val søknadId: UUID,
    val fagsakId: String?,
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
