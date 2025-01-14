package no.nav.hjelpemidler.soknad.db.soknad

import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSakId
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSakId
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Sakstilknytning
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer

fun lagSakstilknytningInfotrygd(fnrBruker: String = lagFødselsnummer()): Sakstilknytning.Infotrygd = Sakstilknytning.Infotrygd(
    sakId = InfotrygdSakId(
        trygdekontornummer = (1..9999).random(4),
        saksblokk = ('A'..'Z').random().toString(),
        saksnummer = (1..9).random(2),
    ),
    fnrBruker = fnrBruker,
)

fun lagSakstilknytningHotsak(): Sakstilknytning.Hotsak = Sakstilknytning.Hotsak(
    sakId = HotsakSakId((1000..9999).random().toString()),
)

private fun IntRange.random(length: Int): String = random().toString().padStart(length, '0')
