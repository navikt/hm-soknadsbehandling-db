package no.nav.hjelpemidler.soknad.db.domain

import java.util.UUID

class UtgåttSøknad(
    val søknadId: UUID,
    val status: Status,
    val fnrBruker: String,
)
