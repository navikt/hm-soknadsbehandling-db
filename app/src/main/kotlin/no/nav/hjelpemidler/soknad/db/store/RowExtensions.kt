package no.nav.hjelpemidler.soknad.db.store

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.InnsenderbehovsmeldingMetadataDto
import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadDto
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSakId
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSakId
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Innsenderbehovsmelding
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.mapping.tilInnsenderbehovsmeldingV2
import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.database.enum
import no.nav.hjelpemidler.database.enumOrNull
import no.nav.hjelpemidler.database.json
import no.nav.hjelpemidler.database.jsonOrNull
import no.nav.hjelpemidler.domain.person.Fødselsnummer
import no.nav.hjelpemidler.soknad.db.sak.tilVedtak

private val log = KotlinLogging.logger {}

fun Row.tilBehovsmeldingType(columnLabel: String = "behovsmeldingType"): BehovsmeldingType =
    enumOrNull<BehovsmeldingType>(columnLabel) ?: BehovsmeldingType.SØKNAD

fun Row.tilSøknadId(columnLabel: String = "soknads_id"): BehovsmeldingId =
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
        journalpostId = stringOrNull("journalpostid"),
        oppgaveId = stringOrNull("oppgaveid"),
        digital = boolean("er_digital"),
        behovsmeldingstype = enum<BehovsmeldingType>("behovsmeldingstype"),
        status = enum<BehovsmeldingStatus>("status"),
        statusEndret = instant("status_endret"),
        data = jsonOrNull<Map<String, Any?>>("data") ?: emptyMap(),
    )
}

fun Row.tilInnsenderbehovsmelding(): Innsenderbehovsmelding {
    return jsonOrNull<Innsenderbehovsmelding>("data_v2")
        ?: tilInnsenderbehovsmeldingV2(json<no.nav.hjelpemidler.behovsmeldingsmodell.v1.Behovsmelding>("data"))
}

fun Row.tilInnsenderbehovsmeldingMetadataDto(): InnsenderbehovsmeldingMetadataDto {
    return InnsenderbehovsmeldingMetadataDto(
        behovsmeldingId = tilSøknadId(),
        innsenderbehovsmelding = tilInnsenderbehovsmelding(),
        fnrInnsender = Fødselsnummer(string("fnr_innsender")),
        behovsmeldingGjelder = string("soknad_gjelder"),
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
