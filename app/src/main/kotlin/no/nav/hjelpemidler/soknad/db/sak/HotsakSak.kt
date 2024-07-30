package no.nav.hjelpemidler.soknad.db.sak

import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSakId
import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.soknad.db.domain.tilSøknadId

fun Row.tilHotsakSak(): HotsakSak = HotsakSak(
    sakId = HotsakSakId(string("saksnummer")),
    søknadId = tilSøknadId(),
    opprettet = instant("created"),
    vedtak = tilVedtak(),
)
