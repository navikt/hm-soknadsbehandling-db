package no.nav.hjelpemidler.soknad.db.sak

import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSakId
import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.soknad.db.domain.tilSøknadId

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
