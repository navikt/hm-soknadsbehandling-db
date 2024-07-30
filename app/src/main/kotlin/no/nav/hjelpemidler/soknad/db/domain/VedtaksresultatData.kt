package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate
import java.util.UUID

data class VedtaksresultatData(
    val søknadId: UUID,
    val fnrBruker: String,
    val trygdekontorNr: String?,
    val saksblokk: String?,
    val saksnr: String?,
    val vedtaksresultat: String? = null,
    val vedtaksdato: LocalDate? = null,
) {
    val saksblokkOgSaksnr: String?
        @JsonIgnore get() =
            if (saksblokk.isNullOrBlank() || saksnr.isNullOrBlank()) {
                null
            } else {
                "$saksblokk$saksnr"
            }

    val fagsakId: String?
        @JsonIgnore get() =
            if (trygdekontorNr.isNullOrBlank() || saksblokk.isNullOrBlank() || saksnr.isNullOrBlank()) {
                null
            } else {
                "$trygdekontorNr$saksblokk$saksnr"
            }
}
