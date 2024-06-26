package no.nav.hjelpemidler.soknad.mottak.db

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.hjelpemidler.soknad.db.db.time
import no.nav.hjelpemidler.soknad.db.domain.FagsakData
import no.nav.hjelpemidler.soknad.db.domain.VedtaksresultatData
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

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

class InfotrygdStorePostgres(private val ds: DataSource) : InfotrygdStore {

    // EndeligJournalført frå Joark vil opprette linja, og denne blir berika seinare av Infotrygd med resultat og vedtaksdato
    override fun lagKnytningMellomFagsakOgSøknad(vedtaksresultatData: VedtaksresultatData): Int =
        time("insert_knytning_mellom_søknad_og_fagsak") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "INSERT INTO V1_INFOTRYGD_DATA (SOKNADS_ID, FNR_BRUKER, TRYGDEKONTORNR, SAKSBLOKK, SAKSNR, VEDTAKSRESULTAT, VEDTAKSDATO ) VALUES (?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                        vedtaksresultatData.søknadId,
                        vedtaksresultatData.fnrBruker,
                        vedtaksresultatData.trygdekontorNr,
                        vedtaksresultatData.saksblokk,
                        vedtaksresultatData.saksnr,
                        null,
                        null,
                    ).asUpdate,
                )
            }
        }

    // Vedtaksresultat vil bli gitt av Infotrygd-poller som har oversikt over søknadId, fnr og fagsakId
    override fun lagreVedtaksresultat(
        søknadId: UUID,
        vedtaksresultat: String,
        vedtaksdato: LocalDate,
        soknadsType: String,
    ): Int =
        time("oppdater_vedtaksresultat") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "UPDATE V1_INFOTRYGD_DATA SET VEDTAKSRESULTAT = ?, VEDTAKSDATO = ?, SOKNADSTYPE = ? WHERE SOKNADS_ID = ?",
                        vedtaksresultat,
                        vedtaksdato,
                        soknadsType,
                        søknadId,
                    ).asUpdate,
                )
            }
        }

    // Brukt for å matche Oebs-data mot eit Infotrygd-resultat
    override fun hentSøknadIdFraVedtaksresultat(
        fnrBruker: String,
        saksblokkOgSaksnr: String,
        vedtaksdato: LocalDate,
    ): UUID? {
        val uuids: List<UUID> = time("hent_søknadid_fra_resultat") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "SELECT SOKNADS_ID FROM V1_INFOTRYGD_DATA WHERE FNR_BRUKER = ? AND SAKSBLOKK = ? AND SAKSNR = ? AND VEDTAKSDATO = ?",
                        fnrBruker,
                        saksblokkOgSaksnr.first(),
                        saksblokkOgSaksnr.takeLast(2),
                        vedtaksdato,
                    ).map {
                        UUID.fromString(it.string("SOKNADS_ID"))
                    }.asList,
                )
            }
        }
        if (uuids.count() != 1) {
            if (uuids.count() > 1) {
                sikkerlogg.info("Fant flere søknader med matchende fnr,saksnr og vedtaksdato, saksblokkogsaksnr: $saksblokkOgSaksnr, vedtaksdato: $vedtaksdato, antall=${uuids.count()} ids: [$uuids]")
            } else {
                sikkerlogg.info("Kan ikke knytte ordrelinje til søknad. saksblokkogsaksnr: $saksblokkOgSaksnr, vedtaksdato: $vedtaksdato")
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
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "SELECT SOKNADS_ID, VEDTAKSDATO FROM V1_INFOTRYGD_DATA WHERE FNR_BRUKER = ? AND SAKSBLOKK = ? AND SAKSNR = ?",
                        fnrBruker,
                        saksblokkOgSaksnr.first(),
                        saksblokkOgSaksnr.takeLast(2),
                    ).map {
                        SøknadIdFraVedtaksresultat(
                            UUID.fromString(it.string("SOKNADS_ID")),
                            it.localDateOrNull("VEDTAKSDATO"),
                        )
                    }.asList,
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
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "SELECT SOKNADS_ID, FNR_BRUKER, TRYGDEKONTORNR, SAKSBLOKK, SAKSNR, VEDTAKSRESULTAT, VEDTAKSDATO FROM V1_INFOTRYGD_DATA WHERE SOKNADS_ID = ?",
                        søknadId,
                    ).map {
                        VedtaksresultatData(
                            søknadId = UUID.fromString(it.string("SOKNADS_ID")),
                            fnrBruker = it.string("FNR_BRUKER"),
                            trygdekontorNr = it.string("TRYGDEKONTORNR"),
                            saksblokk = it.string("SAKSBLOKK"),
                            saksnr = it.string("SAKSNR"),
                            vedtaksresultat = it.stringOrNull("VEDTAKSRESULTAT"),
                            vedtaksdato = it.localDateOrNull("VEDTAKSDATO"),
                        )
                    }.asSingle,
                )
            }
        }
    }

    override fun hentFagsakIdForSøknad(søknadId: UUID): FagsakData? {
        return time("hent_fagsakId_for_soknad") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        """
                            SELECT SOKNADS_ID, TRYGDEKONTORNR, SAKSBLOKK, SAKSNR
                            FROM V1_INFOTRYGD_DATA
                            WHERE
                                SOKNADS_ID = ?
                                AND TRYGDEKONTORNR IS NOT NULL
                                AND SAKSBLOKK IS NOT NULL
                                AND SAKSNR IS NOT NULL
                        """.trimIndent(),
                        søknadId,
                    ).map {
                        FagsakData(
                            søknadId = UUID.fromString(it.string("SOKNADS_ID")),
                            fagsakId = it.string("TRYGDEKONTORNR") + it.string("SAKSBLOKK") + it.string("SAKSNR"),
                        )
                    }.asSingle,
                )
            }
        }
    }

    override fun hentTypeForSøknad(søknadId: UUID): String? {
        return time("hent_type_for_soknad") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        """
                            SELECT SOKNADSTYPE
                            FROM V1_INFOTRYGD_DATA
                            WHERE
                                SOKNADS_ID = ?
                        """.trimIndent(),
                        søknadId,
                    ).map {
                        it.stringOrNull("SOKNADSTYPE")
                    }.asSingle,
                )
            }
        }
    }
}
