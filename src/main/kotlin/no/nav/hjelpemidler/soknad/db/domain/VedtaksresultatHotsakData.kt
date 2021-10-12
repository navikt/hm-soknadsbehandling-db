package no.nav.hjelpemidler.soknad.db.domain

import java.time.LocalDate
import java.util.UUID

internal data class VedtaksresultatHotsakData(
    val søknadId: UUID,
    val saksnr: String?,
    val vedtaksresultat: String? = null,
    val vedtaksdato: LocalDate? = null,
)
