package no.nav.hjelpemidler.soknad.db.domain.kommuneapi

import no.nav.hjelpemidler.soknad.db.domain.kommuneapi.v1.Behovsmelding
import no.nav.hjelpemidler.soknad.db.domain.kommuneapi.v2.Innsenderbehovsmelding
import java.time.LocalDateTime
import java.util.UUID

data class SøknadForKommuneApi(
    val fnrBruker: String,
    val navnBruker: String,
    val fnrInnsender: String?,
    val soknadId: UUID,
    val soknad: Behovsmelding,
    val soknadGjelder: String?,
    val opprettet: LocalDateTime,
)

data class SøknadForKommuneApiV2(
    val fnrBruker: String,
    val navnBruker: String,
    val fnrInnsender: String?,
    val soknadId: UUID,
    val soknad: Innsenderbehovsmelding,
    val soknadGjelder: String?,
    val opprettet: LocalDateTime,
)
