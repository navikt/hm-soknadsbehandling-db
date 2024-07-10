package no.nav.hjelpemidler.soknad.db

import java.time.LocalDate
import java.util.UUID

data class VedtaksresultatDto(
    val søknadId: UUID,
    val vedtaksresultat: String,
    val vedtaksdato: LocalDate,
    val soknadsType: String,
)

data class FnrOgJournalpostIdFinnesDto(
    val fnrBruker: String,
    val journalpostId: Int,
)

data class SøknadFraVedtaksresultatDto(
    val fnrBruker: String,
    val saksblokkOgSaksnr: String,
    val vedtaksdato: LocalDate,
)

data class SøknadFraVedtaksresultatDtoV2(
    val fnrBruker: String,
    val saksblokkOgSaksnr: String,
)

data class SøknadFraHotsakNummerDto(val saksnummer: String)

data class HarVedtakFraHotsakSøknadIdDto(val søknadId: UUID)
