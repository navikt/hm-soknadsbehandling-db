package no.nav.hjelpemidler.behovsmeldingsmodell.sak

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import java.time.Instant

data class InfotrygdSak(
    override val sakId: InfotrygdSakId,
    override val søknadId: BehovsmeldingId,
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
