package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.soknad.db.VedtaksresultatDto
import java.time.LocalDate
import java.util.UUID

fun lagVedtaksresultat1(
    søknadId: UUID = lagSøknadId(),
    fnrBruker: String = "12345678910",
): VedtaksresultatData = VedtaksresultatData(
    søknadId = søknadId,
    fnrBruker = fnrBruker,
    trygdekontorNr = "9999",
    saksblokk = "A",
    saksnr = "01",
)

fun lagVedtaksresultat2(
    søknadId: UUID = lagSøknadId(),
): VedtaksresultatDto = VedtaksresultatDto(
    søknadId = søknadId,
    vedtaksresultat = "I",
    vedtaksdato = LocalDate.now(),
    soknadsType = "HJDAANS",
)
