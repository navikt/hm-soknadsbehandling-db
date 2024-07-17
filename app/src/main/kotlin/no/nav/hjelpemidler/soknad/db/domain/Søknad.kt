package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.database.enum
import java.time.Instant

data class Søknad(
    val søknadId: SøknadId,
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
    val status: Status,
    val statusEndret: Instant,
)

fun Row.tilSøknad(): Søknad {
    return Søknad(
        søknadId = søknadId(),
        søknadOpprettet = instant("soknad_opprettet"),
        søknadEndret = instant("soknad_endret"),
        søknadGjelder = string("soknad_gjelder"),
        fnrInnsender = stringOrNull("fnr_innsender"),
        fnrBruker = string("fnr_bruker"),
        navnBruker = string("navn_bruker"),
        kommunenavn = stringOrNull("kommunenavn"),
        journalpostId = stringOrNull("journalpostid"),
        oppgaveId = stringOrNull("oppgaveid"),
        digital = boolean("er_digital"),
        behovsmeldingstype = enum<BehovsmeldingType>("behovsmeldingstype"),
        status = enum<Status>("status"),
        statusEndret = instant("status_endret"),
    )
}
