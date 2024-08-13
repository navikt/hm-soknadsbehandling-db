package no.nav.hjelpemidler.soknad.db.soknad

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.Behovsmeldingsgrunnlag
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import no.nav.hjelpemidler.soknad.db.domain.lagSøknadId

fun lagBehovsmeldingsgrunnlagDigital(
    status: BehovsmeldingStatus = BehovsmeldingStatus.VENTER_GODKJENNING,
): Behovsmeldingsgrunnlag.Digital = Behovsmeldingsgrunnlag.Digital(
    søknadId = lagSøknadId(),
    status = status,
    fnrBruker = lagFødselsnummer(),
    navnBruker = "Bru K. Er",
    fnrInnsender = lagFødselsnummer(),
    kommunenavn = "TEST",
    behovsmelding = emptyMap(),
    behovsmeldingGjelder = "TEST",
)

fun lagBehovsmeldingsgrunnlagPapir(
    status: BehovsmeldingStatus = BehovsmeldingStatus.ENDELIG_JOURNALFØRT,
): Behovsmeldingsgrunnlag.Papir {
    val fnrBruker = lagFødselsnummer()
    return Behovsmeldingsgrunnlag.Papir(
        søknadId = lagSøknadId(),
        status = status,
        fnrBruker = fnrBruker,
        navnBruker = "Bru K. Er",
        journalpostId = "1",
        sakstilknytning = lagSakstilknytningInfotrygd(fnrBruker),
    )
}
