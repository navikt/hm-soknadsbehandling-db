package no.nav.hjelpemidler.soknad.db.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.hjelpemidler.soknad.db.domain.HotsakTilknytningData
import no.nav.hjelpemidler.soknad.db.domain.VedtaksresultatHotsakData
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

interface HotsakStore {
    fun lagKnytningMellomSakOgSøknad(hotsakTilknytningData: HotsakTilknytningData): Int
    fun lagreVedtaksresultat(
        søknadId: UUID,
        vedtaksresultat: String,
        vedtaksdato: LocalDate,
    ): Int

    fun hentVedtaksresultatForSøknad(søknadId: UUID): VedtaksresultatHotsakData?
    fun hentSøknadsIdForHotsakNummer(saksnummer: String): UUID?
    fun harVedtakForSøknadId(søknadId: UUID): Boolean
    fun hentFagsakIdForSøknad(søknadId: UUID): String?
}

class HotsakStorePostgres(private val ds: DataSource) : HotsakStore {

    override fun lagKnytningMellomSakOgSøknad(hotsakTilknytningData: HotsakTilknytningData): Int =
        time("insert_knytning_mellom_søknad_og_hotsak") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "INSERT INTO V1_HOTSAK_DATA (SOKNADS_ID, SAKSNUMMER, VEDTAKSRESULTAT, VEDTAKSDATO ) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",
                        hotsakTilknytningData.søknadId,
                        hotsakTilknytningData.saksnr,
                        null,
                        null,
                    ).asUpdate,
                )
            }
        }

    override fun lagreVedtaksresultat(
        søknadId: UUID,
        vedtaksresultat: String,
        vedtaksdato: LocalDate,
    ): Int =
        time("oppdater_vedtaksresultat_fra_hotsak") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "UPDATE V1_HOTSAK_DATA SET VEDTAKSRESULTAT = ?, VEDTAKSDATO = ? WHERE SOKNADS_ID = ?",
                        vedtaksresultat,
                        vedtaksdato,
                        søknadId,
                    ).asUpdate,
                )
            }
        }

    override fun hentVedtaksresultatForSøknad(søknadId: UUID): VedtaksresultatHotsakData? {
        return time("hent_søknadid_fra_resultat") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "SELECT SOKNADS_ID, SAKSNUMMER, VEDTAKSRESULTAT, VEDTAKSDATO FROM V1_HOTSAK_DATA WHERE SOKNADS_ID = ?",
                        søknadId,
                    ).map {
                        VedtaksresultatHotsakData(
                            søknadId = UUID.fromString(it.string("SOKNADS_ID")),
                            saksnr = it.string("SAKSNUMMER"),
                            vedtaksresultat = it.stringOrNull("VEDTAKSRESULTAT"),
                            vedtaksdato = it.localDateOrNull("VEDTAKSDATO"),
                        )
                    }.asSingle,
                )
            }
        }
    }

    override fun hentSøknadsIdForHotsakNummer(saksnummer: String): UUID? {
        val søknadsId: UUID? = using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    "SELECT SOKNADS_ID FROM V1_HOTSAK_DATA WHERE SAKSNUMMER = ? ",
                    saksnummer,
                ).map {
                    UUID.fromString(it.string("SOKNADS_ID"))
                }.asSingle,
            )
        }
        return søknadsId
    }

    override fun harVedtakForSøknadId(søknadId: UUID): Boolean {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    "SELECT 1 FROM V1_HOTSAK_DATA WHERE SOKNADS_ID = ? AND VEDTAKSRESULTAT IS NOT NULL",
                    søknadId,
                ).map {
                    true
                }.asSingle,
            )
        } ?: false
    }

    override fun hentFagsakIdForSøknad(søknadId: UUID): String? {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    "SELECT SAKSNUMMER FROM V1_HOTSAK_DATA WHERE SOKNADS_ID = ?",
                    søknadId,
                ).map {
                    it.string("SAKSNUMMER")
                }.asSingle,
            )
        }
    }
}
