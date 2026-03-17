package no.nav.hjelpemidler.soknad.db.soknad

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.Behovsmeldingsgrunnlag
import no.nav.hjelpemidler.behovsmeldingsmodell.Statusendring
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Fagsak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Sakstilknytning
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Vedtaksresultat
import no.nav.hjelpemidler.domain.person.Fødselsnummer
import no.nav.hjelpemidler.soknad.db.exception.BehovsmeldingNotFoundException
import no.nav.hjelpemidler.soknad.db.exception.BehovsmeldingUgyldigStatusException
import no.nav.hjelpemidler.soknad.db.kafka.KafkaClient
import no.nav.hjelpemidler.soknad.db.metrics.Metrics
import no.nav.hjelpemidler.soknad.db.rapportering.epost.ContentType
import no.nav.hjelpemidler.soknad.db.rapportering.epost.EPOST_DIGIHOT
import no.nav.hjelpemidler.soknad.db.rapportering.epost.EpostClient
import no.nav.hjelpemidler.soknad.db.rapportering.epost.HILSEN_DIGIHOT
import no.nav.hjelpemidler.soknad.db.store.Transaction
import java.util.UUID

private val logg = KotlinLogging.logger {}

class SøknadService(
    private val transaction: Transaction,
    private val kafkaClient: KafkaClient,
    private val epostClient: EpostClient,
    private val metrics: Metrics,

) {
    suspend fun lagreBehovsmelding(grunnlag: Behovsmeldingsgrunnlag): Int {
        val søknadId = grunnlag.søknadId
        logg.info { "Lagrer behovsmelding, søknadId: $søknadId, kilde: ${grunnlag.kilde}" }
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
        return when (status) {
            BehovsmeldingStatus.SLETTET -> {
                logg.info { "Sletter søknad, søknadId: $søknadId, status: $status" }
                val (rowsUpdated, formidlersEpost) = transaction {
                    val behovsmelding =
                        søknadStore.finnInnsenderbehovsmelding(søknadId) ?: throw BehovsmeldingNotFoundException(
                            søknadId,
                        )
                    val formidlersEpost = behovsmelding.levering.hjelpemiddelformidler.epost
                    val rowsUpdated = søknadStore.slettSøknad(søknadId)
                    Pair(rowsUpdated, formidlersEpost)
                }
                if (rowsUpdated > 0) {
                    try {
                        varsleOmSlettetBehovsmelding(formidlersEpost)
                        logg.info { "Varslet formidler per epost om at en behovsmelding har blitt slettet ($søknadId)." }
                        metrics.innbyggerSlettetBrukerbekreftelse()
                    } catch (e: Exception) {
                        logg.error(e) { "Epost-varsel til formidler om at en behovsmelding er slettet feilet ($søknadId)." }
                    }
                }
                rowsUpdated
            }

            BehovsmeldingStatus.UTLØPT -> {
                logg.info { "Sletter søknad, søknadId: $søknadId, status: $status" }
                transaction {
                    søknadStore.slettUtløptSøknad(søknadId)
                }
            }

            else -> {
                logg.info { "Oppdaterer søknadsstatus, søknadId: $søknadId, status: $status" }
                transaction {
                    søknadStore.oppdaterStatus(søknadId, status, statusendring.valgteÅrsaker, statusendring.begrunnelse)
                }
            }
        }
    }

    suspend fun varsleOmSlettetBehovsmelding(formidlersEpost: String) {
        epostClient.send(
            avsender = EPOST_DIGIHOT,
            mottaker = formidlersEpost,
            tittel = "Varsel om sak slettet av innbygger",
            innholdstype = ContentType.TEXT,
            innhold = """
                Hei!
                
                En sak du har sendt for bekreftelse er slettet av innbygger. Gå til digital behovsmelding og dine innsendte saker for mer informasjon.
            """.trimIndent() + HILSEN_DIGIHOT,
            lagreIUtboks = true, // TODO skru av etter verifisering
        )
    }

    suspend fun hentStatus(søknadId: BehovsmeldingId): BehovsmeldingStatus {
        return transaction {
            søknadStore.hentStatus(søknadId)
        }
    }

    suspend fun konverterBrukerbekreftelseTilFullmakt(behovsmeldingId: BehovsmeldingId, innsenderFnr: Fødselsnummer) {
        logg.info { "Endrer brukerbekreftelse til fullmakt for behovsmelding $behovsmeldingId" }

        transaction {
            val behovsmelding = søknadStore.finnInnsenderbehovsmelding(behovsmeldingId, innsenderFnr)
                ?: throw BehovsmeldingNotFoundException(behovsmeldingId)

            val nåværendeStatus = søknadStore.hentStatus(behovsmeldingId)
            if (nåværendeStatus != BehovsmeldingStatus.VENTER_GODKJENNING) {
                logg.error { "Kan ikke endre behovsmelding $behovsmeldingId til FULLMAKT fordi nåværende status er $nåværendeStatus" }
                throw BehovsmeldingUgyldigStatusException(
                    behovsmeldingId = behovsmeldingId,
                    nåværendeStatus = nåværendeStatus,
                    forventetStatus = BehovsmeldingStatus.VENTER_GODKJENNING,
                )
            }

            val fullmaktBehovsmelding = behovsmelding.copy(
                bruker = behovsmelding.bruker.copy(
                    signaturtype = no.nav.hjelpemidler.behovsmeldingsmodell.Signaturtype.FULLMAKT,
                ),
            )

            søknadStore.oppdaterStatus(behovsmeldingId, BehovsmeldingStatus.FULLMAKT_AVVENTER_PDF)
            søknadStore.oppdaterBehovsmelding(behovsmeldingId, fullmaktBehovsmelding, innsenderFnr)
        }

        kafkaClient.send(behovsmeldingId, BrukerbekreftelseTilFullmaktAvventerPdf(behovsmeldingId = behovsmeldingId))
        logg.info { "Behovsmelding $behovsmeldingId konvertert fra brukerbekreftelse til fullmakt med status ${BehovsmeldingStatus.FULLMAKT_AVVENTER_PDF}" }
        metrics.brukerbekreftelseTilFullmakt()
    }
}
