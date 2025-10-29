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
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Brukerpassbytte
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Innsenderbehovsmelding
import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.soknad.db.sak.tilVedtak

private val log = KotlinLogging.logger {}

fun Row.tilBehovsmeldingType(columnLabel: String = "behovsmeldingType"): BehovsmeldingType = enumOrNull<BehovsmeldingType>(columnLabel) ?: BehovsmeldingType.SØKNAD

fun Row.tilBehovsmeldingId(columnLabel: String = "soknads_id"): BehovsmeldingId = uuid(columnLabel)

fun Row.tilSøknad(): SøknadDto {
    return SøknadDto(
        søknadId = tilBehovsmeldingId(),
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
    )
}

fun Row.tilInnsenderbehovsmelding(): Innsenderbehovsmelding = json<Innsenderbehovsmelding>("data_v2")

fun Row.tilBrukerpassbytte(): Brukerpassbytte? = jsonOrNull<Brukerpassbytte>("data_v2")

fun Row.tilInnsenderbehovsmeldingMetadataDto(): InnsenderbehovsmeldingMetadataDto = InnsenderbehovsmeldingMetadataDto(
    behovsmeldingId = tilBehovsmeldingId(),
    innsenderbehovsmelding = tilInnsenderbehovsmelding(),
    fnrInnsender = fødselsnummer("fnr_innsender"),
    behovsmeldingGjelder = string("soknad_gjelder"),
)

fun Row.tilHotsakSak(): HotsakSak = HotsakSak(
    sakId = HotsakSakId(string("saksnummer")),
    søknadId = tilBehovsmeldingId(),
    opprettet = instant("created"),
    vedtak = tilVedtak(),
)

fun Row.tilInfotrygdSak(): InfotrygdSak {
    val trygdekontornummer = string("trygdekontornr")
    val saksblokk = string("saksblokk")
    val saksnummer = string("saksnr")
    return InfotrygdSak(
        sakId = InfotrygdSakId("$trygdekontornummer$saksblokk$saksnummer"),
        søknadId = tilBehovsmeldingId(),
        opprettet = instant("created"),
        vedtak = tilVedtak(),
        fnrBruker = string("fnr_bruker"),
        trygdekontornummer = trygdekontornummer,
        saksblokk = saksblokk,
        saksnummer = saksnummer,
        søknadstype = stringOrNull("soknadstype"),
    )
}
