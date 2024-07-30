package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadId
import no.nav.hjelpemidler.behovsmeldingsmodell.TilknyttetSøknad
import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.database.enum
import no.nav.hjelpemidler.database.jsonOrNull
import java.time.Instant

data class Søknad(
    override val søknadId: SøknadId,
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
    val data: JsonNode,
) : TilknyttetSøknad

fun Row.tilSøknad(): Søknad {
    return Søknad(
        søknadId = tilSøknadId(),
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
        status = enum<BehovsmeldingStatus>("status"),
        statusEndret = instant("status_endret"),
        data = jsonOrNull<JsonNode>("data") ?: NullNode.getInstance(),
    )
}
