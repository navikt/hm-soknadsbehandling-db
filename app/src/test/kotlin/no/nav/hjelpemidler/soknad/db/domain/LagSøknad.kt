package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.soknad.db.mockSøknad
import java.util.UUID

fun lagSøknadId(): UUID = UUID.randomUUID()

fun lagSøknad(
    søknadId: UUID = lagSøknadId(),
    status: BehovsmeldingStatus = BehovsmeldingStatus.VENTER_GODKJENNING,
): SøknadData = mockSøknad(søknadId, status)

fun lagPapirsøknad(
    søknadId: UUID = lagSøknadId(),
    fnrBruker: String = "12345678910",
): PapirSøknadData =
    PapirSøknadData(
        søknadId = søknadId,
        journalpostId = "1",
        status = BehovsmeldingStatus.ENDELIG_JOURNALFØRT,
        fnrBruker = fnrBruker,
        navnBruker = "Fornavn Etternavn",
    )
