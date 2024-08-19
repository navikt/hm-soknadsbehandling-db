package no.nav.hjelpemidler.soknad.db.store

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadDto
import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadId
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSakId
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSakId
import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.database.enum
import no.nav.hjelpemidler.database.enumOrNull
import no.nav.hjelpemidler.database.jsonOrNull
import no.nav.hjelpemidler.soknad.db.sak.tilVedtak

fun Row.tilBehovsmeldingType(columnLabel: String = "behovsmeldingType"): BehovsmeldingType =
    enumOrNull<BehovsmeldingType>(columnLabel) ?: BehovsmeldingType.SØKNAD

fun Row.tilSøknadId(columnLabel: String = "soknads_id"): SøknadId =
    uuid(columnLabel)

fun Row.tilSøknad(): SøknadDto {
    return SøknadDto(
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
        data = jsonOrNull<Map<String, Any?>>("data") ?: emptyMap(),
    )
}

fun Row.tilHotsakSak(): HotsakSak = HotsakSak(
    sakId = HotsakSakId(string("saksnummer")),
    søknadId = tilSøknadId(),
    opprettet = instant("created"),
    vedtak = tilVedtak(),
)

fun Row.tilInfotrygdSak(): InfotrygdSak {
    val trygdekontornummer = string("trygdekontornr")
    val saksblokk = string("saksblokk")
    val saksnummer = string("saksnr")
    return InfotrygdSak(
        sakId = InfotrygdSakId("$trygdekontornummer$saksblokk$saksnummer"),
        søknadId = tilSøknadId(),
        opprettet = instant("created"),
        vedtak = tilVedtak(),
        fnrBruker = string("fnr_bruker"),
        trygdekontornummer = trygdekontornummer,
        saksblokk = saksblokk,
        saksnummer = saksnummer,
        søknadstype = stringOrNull("soknadstype"),
    )
}
