package no.nav.hjelpemidler.soknad.db.store

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.soknad.db.domain.FagsakData
import no.nav.hjelpemidler.soknad.db.domain.VedtaksresultatData
import java.time.LocalDate
import java.util.UUID

internal interface InfotrygdStore {
    fun lagKnytningMellomFagsakOgSøknad(vedtaksresultatData: VedtaksresultatData): Int
    fun lagreVedtaksresultat(
        søknadId: UUID,
        vedtaksresultat: String,
        vedtaksdato: LocalDate,
        soknadsType: String,
    ): Int

    fun hentSøknadIdFraVedtaksresultat(fnrBruker: String, saksblokkOgSaksnr: String, vedtaksdato: LocalDate): UUID?
    fun hentSøknadIdFraVedtaksresultatV2(
        fnrBruker: String,
        saksblokkOgSaksnr: String,
    ): List<InfotrygdStorePostgres.SøknadIdFraVedtaksresultat>

    fun hentVedtaksresultatForSøknad(søknadId: UUID): VedtaksresultatData?
    fun hentFagsakIdForSøknad(søknadId: UUID): FagsakData?
    fun hentTypeForSøknad(søknadId: UUID): String?
}

private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class InfotrygdStorePostgres(private val tx: JdbcOperations) : InfotrygdStore {
    // EndeligJournalført frå Joark vil opprette linja, og denne blir berika seinare av Infotrygd med resultat og vedtaksdato
    override fun lagKnytningMellomFagsakOgSøknad(vedtaksresultatData: VedtaksresultatData): Int =
        time("insert_knytning_mellom_søknad_og_fagsak") {
            tx.update(
                """
                    INSERT INTO v1_infotrygd_data (soknads_id, fnr_bruker, trygdekontornr, saksblokk, saksnr)
                    VALUES (:soknadId, :fnrBruker, :trygdekontorNr, :saksblokk, :saksnr)
                    ON CONFLICT DO NOTHING
                """.trimIndent(),
                mapOf(
                    "soknadId" to vedtaksresultatData.søknadId,
                    "fnrBruker" to vedtaksresultatData.fnrBruker,
                    "trygdekontorNr" to vedtaksresultatData.trygdekontorNr,
                    "saksblokk" to vedtaksresultatData.saksblokk,
                    "saksnr" to vedtaksresultatData.saksnr,
                ),
            ).actualRowCount
        }

    // Vedtaksresultat vil bli gitt av Infotrygd-poller som har oversikt over søknadId, fnr og fagsakId
    override fun lagreVedtaksresultat(
        søknadId: UUID,
        vedtaksresultat: String,
        vedtaksdato: LocalDate,
        søknadstype: String,
    ): Int =
        time("oppdater_vedtaksresultat") {
            tx.update(
                """
                    UPDATE v1_infotrygd_data
                    SET vedtaksresultat = :vedtaksresultat,
                        vedtaksdato = :vedtaksdato,
                        soknadstype = :soknadstype
                    WHERE soknads_id = :soknadId
                """.trimIndent(),
                mapOf(
                    "vedtaksresultat" to vedtaksresultat,
                    "vedtaksdato" to vedtaksdato,
                    "soknadstype" to søknadstype,
                    "soknadId" to søknadId,
                ),
            ).actualRowCount
        }

    // Brukt for å matche OEBS-data mot et Infotrygd-resultat
    override fun hentSøknadIdFraVedtaksresultat(
        fnrBruker: String,
        saksblokkOgSaksnr: String,
        vedtaksdato: LocalDate,
    ): UUID? {
        val uuids: List<UUID> = time("hent_søknadid_fra_resultat") {
            tx.list(
                """
                    SELECT soknads_id
                    FROM v1_infotrygd_data
                    WHERE fnr_bruker = :fnrBruker
                      AND saksblokk = :saksblokk
                      AND saksnr = :saksnr
                      AND vedtaksdato = :vedtaksdato
                """.trimIndent(),
                mapOf(
                    "fnrBruker" to fnrBruker,
                    "saksblokk" to saksblokkOgSaksnr.first(),
                    "saksnr" to saksblokkOgSaksnr.takeLast(2),
                    "vedtaksdato" to vedtaksdato,
                ),
            ) { it.uuid("soknads_id") }
        }
        if (uuids.count() != 1) {
            if (uuids.count() > 1) {
                sikkerlogg.info { "Fant flere søknader med matchende fnr,saksnr og vedtaksdato, saksblokkogsaksnr: $saksblokkOgSaksnr, vedtaksdato: $vedtaksdato, antall=${uuids.count()} ids: [$uuids]" }
            } else {
                sikkerlogg.info { "Kan ikke knytte ordrelinje til søknad. saksblokkogsaksnr: $saksblokkOgSaksnr, vedtaksdato: $vedtaksdato" }
            }
            return null
        }
        return uuids[0]
    }

    // Brukt for å matche Oebs-data mot eit Infotrygd-resultat i alternativ flyt
    override fun hentSøknadIdFraVedtaksresultatV2(
        fnrBruker: String,
        saksblokkOgSaksnr: String,
    ): List<SøknadIdFraVedtaksresultat> {
        return time("hent_søknadid_fra_resultat") {
            tx.list(
                """
                    SELECT soknads_id, vedtaksdato
                    FROM v1_infotrygd_data
                    WHERE fnr_bruker = :fnrBruker
                      AND saksblokk = :saksblokk
                      AND saksnr = :saksnr
                """.trimIndent(),
                mapOf(
                    "fnrBruker" to fnrBruker,
                    "saksblokk" to saksblokkOgSaksnr.first(),
                    "saksnr" to saksblokkOgSaksnr.takeLast(2),
                ),
            ) {
                SøknadIdFraVedtaksresultat(
                    it.uuid("soknads_id"),
                    it.localDateOrNull("vedtaksdato"),
                )
            }
        }
    }

    data class SøknadIdFraVedtaksresultat(
        val søknadId: UUID,
        val vedtaksDato: LocalDate?,
    )

    override fun hentVedtaksresultatForSøknad(søknadId: UUID): VedtaksresultatData? {
        return time("hent_søknadid_fra_resultat") {
            tx.singleOrNull(
                """
                    SELECT soknads_id, fnr_bruker, trygdekontornr, saksblokk, saksnr, vedtaksresultat, vedtaksdato
                    FROM v1_infotrygd_data
                    WHERE soknads_id = :soknadId
                """.trimIndent(),
                mapOf("soknadId" to søknadId),
            ) {
                VedtaksresultatData(
                    søknadId = it.uuid("SOKNADS_ID"),
                    fnrBruker = it.string("FNR_BRUKER"),
                    trygdekontorNr = it.string("TRYGDEKONTORNR"),
                    saksblokk = it.string("SAKSBLOKK"),
                    saksnr = it.string("SAKSNR"),
                    vedtaksresultat = it.stringOrNull("VEDTAKSRESULTAT"),
                    vedtaksdato = it.localDateOrNull("VEDTAKSDATO"),
                )
            }
        }
    }

    override fun hentFagsakIdForSøknad(søknadId: UUID): FagsakData? {
        return time("hent_fagsakId_for_soknad") {
            tx.singleOrNull(
                """
                    SELECT soknads_id, trygdekontornr, saksblokk, saksnr
                    FROM v1_infotrygd_data
                    WHERE soknads_id = :soknadId
                      AND trygdekontornr IS NOT NULL
                      AND saksblokk IS NOT NULL
                      AND saksnr IS NOT NULL
                """.trimIndent(),
                mapOf("soknadId" to søknadId),
            ) {
                FagsakData(
                    søknadId = it.uuid("SOKNADS_ID"),
                    fagsakId = it.string("TRYGDEKONTORNR") + it.string("SAKSBLOKK") + it.string("SAKSNR"),
                )
            }
        }
    }

    override fun hentTypeForSøknad(søknadId: UUID): String? {
        return time("hent_type_for_soknad") {
            tx.singleOrNull(
                """
                    SELECT soknadstype
                    FROM v1_infotrygd_data
                    WHERE soknads_id = :soknadId
                """.trimIndent(),
                mapOf("soknadId" to søknadId),
            ) { it.stringOrNull("soknadstype") }
        }
    }
}
