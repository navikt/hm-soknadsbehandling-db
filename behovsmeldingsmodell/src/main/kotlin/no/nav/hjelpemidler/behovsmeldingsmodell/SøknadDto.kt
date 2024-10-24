package no.nav.hjelpemidler.behovsmeldingsmodell

import java.time.Instant
import java.util.UUID

/**
 * Brukes bla. ved henting av behovsmelding i hm-soknadsbehandling.
 *
 * todo -> Kunne vi gitt klassen en mindre generisk navn?
 */
data class SøknadDto(
    override val søknadId: UUID,
    val søknadOpprettet: Instant,
    val søknadEndret: Instant,
    val søknadGjelder: String,
    val fnrInnsender: String?,
    val fnrBruker: String,
    val navnBruker: String,
    val journalpostId: String?,
    val oppgaveId: String?,
    val digital: Boolean,
    val behovsmeldingstype: BehovsmeldingType,
    val status: BehovsmeldingStatus,
    val statusEndret: Instant,
    val data: Map<String, Any?>,
) : TilknyttetSøknad
