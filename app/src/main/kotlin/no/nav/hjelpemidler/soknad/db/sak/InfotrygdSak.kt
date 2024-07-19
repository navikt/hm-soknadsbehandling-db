package no.nav.hjelpemidler.soknad.db.sak

import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.soknad.db.domain.SøknadId
import no.nav.hjelpemidler.soknad.db.domain.tilSøknadId
import java.time.Instant

data class InfotrygdSak(
    override val sakId: InfotrygdSakId,
    override val søknadId: SøknadId,
    override val opprettet: Instant,
    override val vedtak: Vedtak?,
    val fnrBruker: String,
    val trygdekontornummer: String,
    val saksblokk: String,
    val saksnummer: String,
    val søknadstype: String?,
) : Fagsak {
    override val system: Fagsak.System = Fagsak.System.INFOTRYGD
}

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
