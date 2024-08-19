package no.nav.hjelpemidler.behovsmeldingsmodell

import java.time.Instant
import java.util.UUID

/**
 * Bruker bla. ved henting av behovsmelding i hm-soknadsbehandling.
 */
data class SøknadDto(
    override val søknadId: UUID,
    val søknadOpprettet: Instant,
    val søknadEndret: Instant,
    val søknadGjelder: String,
    val fnrInnsender: String?,
    val fnrBruker: String,
    val navnBruker: String,
    val kommunenavn: String?,
    val journalpostId: String?,
    val oppgaveId: String?,
    val digital: Boolean,
    val behovsmeldingstype: BehovsmeldingType,
    val status: BehovsmeldingStatus,
    val statusEndret: Instant,
    val data: Map<String, Any?>,
) : TilknyttetSøknad
