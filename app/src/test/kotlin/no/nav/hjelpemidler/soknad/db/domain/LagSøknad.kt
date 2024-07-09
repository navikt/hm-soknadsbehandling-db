package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.soknad.db.mockSøknad
import java.util.UUID

fun lagSøknadId(): UUID = UUID.randomUUID()

fun lagSøknad(
    søknadId: UUID = lagSøknadId(),
): SoknadData = mockSøknad(søknadId)

fun lagPapirsøknad(
    søknadId: UUID = lagSøknadId(),
    fnrBruker: String = "12345678910",
): PapirSøknadData =
    PapirSøknadData(
        fnrBruker = fnrBruker,
        soknadId = søknadId,
        status = Status.ENDELIG_JOURNALFØRT,
        journalpostid = 1,
        navnBruker = "Fornavn Etternavn",
    )
