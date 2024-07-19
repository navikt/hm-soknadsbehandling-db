package no.nav.hjelpemidler.behovsmeldingsmodell.sak

import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadId
import java.time.Instant

data class HotsakSak(
    override val sakId: HotsakSakId,
    override val søknadId: SøknadId,
    override val opprettet: Instant,
    override val vedtak: Vedtak? = null,
) : Fagsak {
    override val system: Fagsak.System = Fagsak.System.HOTSAK
}
