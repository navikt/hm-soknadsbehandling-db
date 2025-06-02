package no.nav.hjelpemidler.soknad.db.soknad

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.Behovsmeldingsgrunnlag
import no.nav.hjelpemidler.behovsmeldingsmodell.Statusendring
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Fagsak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Sakstilknytning
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Vedtaksresultat
import no.nav.hjelpemidler.logging.teamInfo
import no.nav.hjelpemidler.soknad.db.store.Transaction
import java.util.UUID

private val logg = KotlinLogging.logger {}

class SøknadService(private val transaction: Transaction) {
    suspend fun lagreBehovsmelding(grunnlag: Behovsmeldingsgrunnlag): Int {
        val søknadId = grunnlag.søknadId
        logg.info { "Lagrer behovsmelding, søknadId: $søknadId, kilde: ${grunnlag.kilde}" }
        logg.teamInfo { "LOGG_TEST: test av teamlog" }
        return when (grunnlag) {
            is Behovsmeldingsgrunnlag.Digital -> transaction {
                søknadStore.lagreBehovsmelding(grunnlag)
            }

            is Behovsmeldingsgrunnlag.Papir -> {
                val journalpostId = grunnlag.journalpostId
                val fnrOgJournalpostIdFinnes = transaction {
                    søknadStore.fnrOgJournalpostIdFinnes(grunnlag.fnrBruker, journalpostId)
                }
                if (fnrOgJournalpostIdFinnes) {
                    logg.warn { "En søknad med dette fødselsnummeret og journalpostId: $journalpostId er allerede lagret, søknadId: $søknadId" }
                    return 0
                }
                transaction {
                    val rowsUpdated = søknadStore.lagrePapirsøknad(grunnlag)
                    val sakstilknytning = grunnlag.sakstilknytning
                    if (sakstilknytning != null) {
                        infotrygdStore.lagKnytningMellomSakOgSøknad(
                            søknadId,
                            sakstilknytning.sakId,
                            sakstilknytning.fnrBruker,
                        )
                    }
                    rowsUpdated
                }
            }
        }
    }

    suspend fun finnSak(søknadId: BehovsmeldingId): Fagsak? {
        return transaction {
            hotsakStore.finnSak(søknadId) ?: infotrygdStore.finnSak(søknadId)
        }
    }

    suspend fun lagreSakstilknytning(
        søknadId: BehovsmeldingId,
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
        søknadId: BehovsmeldingId,
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

    suspend fun oppdaterStatus(
        søknadId: UUID,
        statusendring: Statusendring,
    ): Int {
        val status = statusendring.status
        return transaction {
            when (status) {
                BehovsmeldingStatus.SLETTET -> {
                    logg.info { "Sletter søknad, søknadId: $søknadId, status: $status" }
                    søknadStore.slettSøknad(søknadId)
                }

                BehovsmeldingStatus.UTLØPT -> {
                    logg.info { "Sletter søknad, søknadId: $søknadId, status: $status" }
                    søknadStore.slettUtløptSøknad(søknadId)
                }

                else -> {
                    logg.info { "Oppdaterer søknadsstatus, søknadId: $søknadId, status: $status" }
                    søknadStore.oppdaterStatus(søknadId, status, statusendring.valgteÅrsaker, statusendring.begrunnelse)
                }
            }
        }
    }
}
