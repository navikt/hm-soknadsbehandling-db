package no.nav.hjelpemidler.behovsmeldingsmodell.sak

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import java.time.Instant

data class HotsakSak(
    override val sakId: HotsakSakId,
    override val søknadId: BehovsmeldingId,
    override val opprettet: Instant,
    override val vedtak: Vedtak? = null,
) : Fagsak {
    override val system: Fagsak.System = Fagsak.System.HOTSAK
}
