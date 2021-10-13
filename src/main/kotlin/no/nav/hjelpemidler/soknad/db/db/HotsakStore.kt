package no.nav.hjelpemidler.soknad.db.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.hjelpemidler.soknad.db.domain.HotsakTilknytningData
import no.nav.hjelpemidler.soknad.db.domain.VedtaksresultatHotsakData
import no.nav.hjelpemidler.soknad.db.metrics.Prometheus
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

internal interface HotsakStore {
    fun lagKnytningMellomSakOgSøknad(hotsakTilknytningData: HotsakTilknytningData): Int
    fun lagreVedtaksresultat(
        søknadId: UUID,
        vedtaksresultat: String,
        vedtaksdato: LocalDate
    ): Int

    fun hentVedtaksresultatForSøknad(søknadId: UUID): VedtaksresultatHotsakData?
}

private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class HotsakStorePostgres(private val ds: DataSource) : HotsakStore {

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
                    ).asUpdate
                )
            }
        }

    override fun lagreVedtaksresultat(
        søknadId: UUID,
        vedtaksresultat: String,
        vedtaksdato: LocalDate
    ): Int =
        time("oppdater_vedtaksresultat_fra_hotsak") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "UPDATE V1_HOTSAK_DATA SET VEDTAKSRESULTAT = ?, VEDTAKSDATO = ? WHERE SOKNADS_ID = ?",
                        vedtaksresultat,
                        vedtaksdato,
                        søknadId,
                    ).asUpdate
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
                    }.asSingle
                )
            }
        }
    }

    private inline fun <T : Any?> time(queryName: String, function: () -> T) =
        Prometheus.dbTimer.labels(queryName).startTimer().let { timer ->
            function().also {
                timer.observeDuration()
            }
        }
}
