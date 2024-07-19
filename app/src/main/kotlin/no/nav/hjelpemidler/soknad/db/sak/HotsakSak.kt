package no.nav.hjelpemidler.soknad.db.sak

import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.soknad.db.domain.SøknadId
import no.nav.hjelpemidler.soknad.db.domain.tilSøknadId
import java.time.Instant

data class HotsakSak(
    override val sakId: HotsakSakId,
    override val søknadId: SøknadId,
    override val opprettet: Instant,
    override val vedtak: Vedtak? = null,
) : Fagsak {
    override val system: Fagsak.System = Fagsak.System.HOTSAK
}

fun Row.tilHotsakSak(): HotsakSak = HotsakSak(
    sakId = HotsakSakId(string("saksnummer")),
    søknadId = tilSøknadId(),
    opprettet = instant("created"),
    vedtak = tilVedtak(),
)
