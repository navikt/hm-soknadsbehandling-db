package no.nav.hjelpemidler.soknad.db

import java.time.LocalDate
import java.util.UUID

data class VedtaksresultatDto(
    val søknadId: UUID,
    val vedtaksresultat: String,
    val vedtaksdato: LocalDate,
    val soknadsType: String,
)

data class SøknadFraVedtaksresultatDtoV1(
    val fnrBruker: String,
    val saksblokkOgSaksnr: String,
    val vedtaksdato: LocalDate,
)

data class SøknadFraVedtaksresultatDtoV2(
    val fnrBruker: String,
    val saksblokkOgSaksnr: String,
)
