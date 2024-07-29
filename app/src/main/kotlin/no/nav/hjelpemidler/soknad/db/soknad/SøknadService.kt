package no.nav.hjelpemidler.soknad.db.soknad

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadId
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Fagsak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Sakstilknytning
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Vedtaksresultat
import no.nav.hjelpemidler.soknad.db.store.Transaction

private val logg = KotlinLogging.logger {}

class SøknadService(private val transaction: Transaction) {
    suspend fun finnSak(søknadId: SøknadId): Fagsak? {
        return transaction {
            hotsakStore.finnSak(søknadId) ?: infotrygdStore.finnSak(søknadId)
        }
    }

    suspend fun lagreSakstilknytning(
        søknadId: SøknadId,
        sakstilknytning: Sakstilknytning,
    ): Int {
        logg.info { "Knytter sakId: ${sakstilknytning.sakId} til søknadId: $søknadId, system: ${sakstilknytning.system}" }
        return transaction {
            when (sakstilknytning) {
                is Sakstilknytning.Hotsak -> hotsakStore.lagKnytningMellomSakOgSøknad(
                    søknadId = søknadId,
                    sakId = sakstilknytning.sakId,
                )

                is Sakstilknytning.Infotrygd -> infotrygdStore.lagKnytningMellomSakOgSøknad(
                    søknadId = søknadId,
                    sakId = sakstilknytning.sakId,
                    fnrBruker = sakstilknytning.fnrBruker,
                )
            }
        }
    }

    suspend fun lagreVedtaksresultat(
        søknadId: SøknadId,
        vedtaksresultat: Vedtaksresultat,
    ): Int {
        val nyStatus = BehovsmeldingStatus.fraVedtaksresultat(vedtaksresultat)
        logg.info { "Lagrer vedtaksresultat for søknadId: $søknadId, system: ${vedtaksresultat.system}, nyStatus: $nyStatus" }
        return transaction {
            if (søknadStore.oppdaterStatus(søknadId, nyStatus) == 0) {
                logg.warn { "Status for søknadId: $søknadId er allerede $nyStatus" }
            }
            when (vedtaksresultat) {
                is Vedtaksresultat.Hotsak -> hotsakStore.lagreVedtaksresultat(
                    søknadId = søknadId,
                    vedtaksresultat = vedtaksresultat.vedtaksresultat,
                    vedtaksdato = vedtaksresultat.vedtaksdato,
                )

                is Vedtaksresultat.Infotrygd -> infotrygdStore.lagreVedtaksresultat(
                    søknadId = søknadId,
                    vedtaksresultat = vedtaksresultat.vedtaksresultat,
                    vedtaksdato = vedtaksresultat.vedtaksdato,
                    søknadstype = vedtaksresultat.søknadstype,
                )
            }
        }
    }
}
