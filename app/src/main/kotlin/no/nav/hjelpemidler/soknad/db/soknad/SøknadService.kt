package no.nav.hjelpemidler.soknad.db.soknad

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.soknad.db.domain.SøknadId
import no.nav.hjelpemidler.soknad.db.sak.Fagsak
import no.nav.hjelpemidler.soknad.db.sak.Sakstilknytning
import no.nav.hjelpemidler.soknad.db.sak.Vedtaksresultat
import no.nav.hjelpemidler.soknad.db.store.Transaction

private val logg = KotlinLogging.logger {}

class SøknadService(private val transaction: Transaction) {
    suspend fun finnSak(søknadId: SøknadId): Fagsak? {
        return transaction {
            hotsakStore.finnSak(søknadId) ?: infotrygdStore.finnSak(søknadId)
        }
    }

    suspend fun lagreSakstilknytning(søknadId: SøknadId, sakstilknytning: Sakstilknytning): Int {
        logg.info { "Knytter sakId: ${sakstilknytning.sakId} til søknadId: $søknadId, system: ${sakstilknytning.system}" }
        return when (sakstilknytning) {
            is Sakstilknytning.Hotsak -> transaction {
                hotsakStore.lagKnytningMellomSakOgSøknad(
                    søknadId = søknadId,
                    sakId = sakstilknytning.sakId,
                )
            }

            is Sakstilknytning.Infotrygd -> transaction {
                infotrygdStore.lagKnytningMellomSakOgSøknad(
                    søknadId = søknadId,
                    sakId = sakstilknytning.sakId,
                    fnrBruker = sakstilknytning.fnrBruker,
                )
            }
        }
    }

    suspend fun lagreVedtaksresultat(søknadId: SøknadId, vedtaksresultat: Vedtaksresultat): Int {
        logg.info { "Lagrer vedtaksresultat for søknadId: $søknadId, system: ${vedtaksresultat.system}" }
        return when (vedtaksresultat) {
            is Vedtaksresultat.Hotsak -> transaction {
                hotsakStore.lagreVedtaksresultat(
                    søknadId = søknadId,
                    vedtaksresultat = vedtaksresultat.vedtaksresultat,
                    vedtaksdato = vedtaksresultat.vedtaksdato,
                )
            }

            is Vedtaksresultat.Infotrygd -> transaction {
                infotrygdStore.lagreVedtaksresultat(
                    søknadId = søknadId,
                    vedtaksresultat = vedtaksresultat.vedtaksresultat,
                    vedtaksdato = vedtaksresultat.vedtaksdato,
                    søknadstype = vedtaksresultat.søknadstype,
                )
            }
        }
    }
}
