package no.nav.hjelpemidler.soknad.db.db

import com.fasterxml.jackson.databind.ObjectMapper
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.hjelpemidler.soknad.db.JacksonMapper
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.domain.Søknadsdata
import no.nav.hjelpemidler.soknad.db.metrics.Prometheus
import org.intellij.lang.annotations.Language
import java.util.Date
import java.util.UUID
import javax.sql.DataSource

private const val UKER_TILGJENGELIG_ETTER_ENDELIG_STATUS = 4

internal interface SøknadStoreFormidler {
    fun hentSøknaderForFormidler(
        fnrFormidler: String,
        ukerEtterEndeligStatus: Int = UKER_TILGJENGELIG_ETTER_ENDELIG_STATUS
    ): List<SoknadForFormidler>

    fun hentSøknadForFormidler(
        fnrFormidler: String,
        soknadId: UUID,
        ukerEtterEndeligStatus: Int = UKER_TILGJENGELIG_ETTER_ENDELIG_STATUS
    ): SoknadForFormidler?
}

private val objectMapper: ObjectMapper = JacksonMapper.objectMapper

internal class SøknadStoreFormidlerPostgres(private val dataSource: DataSource) : SøknadStoreFormidler {

    override fun hentSøknaderForFormidler(fnrFormidler: String, ukerEtterEndeligStatus: Int): List<SoknadForFormidler> {
        @Language("PostgreSQL") val statement =
            """
                SELECT soknad.SOKNADS_ID, soknad.CREATED, soknad.UPDATED, soknad.DATA, soknad.FNR_BRUKER, soknad.NAVN_BRUKER, status.STATUS, 
                (CASE WHEN EXISTS (
                    SELECT 1 FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID AND STATUS IN  ('GODKJENT_MED_FULLMAKT')
                ) THEN true ELSE false END) as fullmakt
                FROM V1_SOKNAD AS soknad
                LEFT JOIN V1_STATUS AS status
                ON status.ID = (
                    SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID ORDER BY created DESC LIMIT 1
                )
                WHERE soknad.FNR_INNSENDER = ?
                AND soknad.created > '2021-05-07'
                AND (
                    status.STATUS NOT IN ('SLETTET', 'UTLØPT', 'VEDTAKSRESULTAT_AVSLÅTT', 'VEDTAKSRESULTAT_ANNET', 'UTSENDING_STARTET')
                    OR (status.CREATED + interval '$ukerEtterEndeligStatus week') > now()
                )
                AND NOT (
                    -- Hvis vi har stått fast i status positivt vedtak i fire uker, og vedtaket kom før de siste fiksene våre så fjerner vi de fra formidleroversikten.
                    -- Dette trengs pga. hvordan vi har kastet ordrelinjer som ikke kunne knyttes til sak, og da ble man hengende igjen for alltid.
                    status.STATUS IN ('VEDTAKSRESULTAT_INNVILGET', 'VEDTAKSRESULTAT_MUNTLIG_INNVILGET', 'VEDTAKSRESULTAT_DELVIS_INNVILGET')
                    AND status.CREATED < '2022-02-14' -- Dagen etter vi lanserte de siste fiksene
                    AND status.CREATED < (now() - interval '4 week') -- Vises i maks fire uker etter vedtak
                )
                ORDER BY soknad.UPDATED DESC
            """

        return time("hent_soknader_for_formidler") {
            using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        statement,
                        fnrFormidler,
                    ).map {
                        SoknadForFormidler(
                            søknadId = UUID.fromString(it.string("SOKNADS_ID")),
                            status = Status.valueOf(it.string("STATUS")),
                            fullmakt = it.boolean("fullmakt"),
                            datoOpprettet = it.sqlTimestamp("created"),
                            datoOppdatert = when {
                                it.sqlTimestampOrNull("updated") != null -> it.sqlTimestamp("updated")
                                else -> it.sqlTimestamp("created")
                            },
                            fnrBruker = it.string("FNR_BRUKER"),
                            navnBruker = it.stringOrNull("NAVN_BRUKER")
                        )
                    }.asList
                )
            }
        }
    }

    override fun hentSøknadForFormidler(
        fnrFormidler: String,
        soknadId: UUID,
        ukerEtterEndeligStatus: Int
    ): SoknadForFormidler? {
        @Language("PostgreSQL") val statement =
            """
                SELECT soknad.SOKNADS_ID, soknad.CREATED, soknad.UPDATED, soknad.DATA, soknad.FNR_BRUKER, soknad.NAVN_BRUKER, status.STATUS, 
                (CASE WHEN EXISTS (
                    SELECT 1 FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID AND STATUS IN  ('GODKJENT_MED_FULLMAKT')
                ) THEN true ELSE false END) as fullmakt
                FROM V1_SOKNAD AS soknad
                LEFT JOIN V1_STATUS AS status
                ON status.ID = (
                    SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID ORDER BY created DESC LIMIT 1
                )
                WHERE soknad.FNR_INNSENDER = :fnrInnsender AND soknad.SOKNADS_ID = :soknadId
                AND soknad.created > '2021-05-07'
                AND (
                    status.STATUS NOT IN ('SLETTET', 'UTLØPT', 'VEDTAKSRESULTAT_AVSLÅTT', 'VEDTAKSRESULTAT_ANNET', 'UTSENDING_STARTET')
                    OR (status.CREATED + interval '$ukerEtterEndeligStatus week') > now()
                )
                AND NOT (
                    -- Hvis vi har stått fast i status positivt vedtak i fire uker, og vedtaket kom før de siste fiksene våre så fjerner vi de fra formidleroversikten.
                    -- Dette trengs pga. hvordan vi har kastet ordrelinjer som ikke kunne knyttes til sak, og da ble man hengende igjen for alltid.
                    status.STATUS IN ('VEDTAKSRESULTAT_INNVILGET', 'VEDTAKSRESULTAT_MUNTLIG_INNVILGET', 'VEDTAKSRESULTAT_DELVIS_INNVILGET')
                    AND status.CREATED < '2022-02-14' -- Dagen etter vi lanserte de siste fiksene
                    AND status.CREATED < (now() - interval '4 week') -- Vises i maks fire uker etter vedtak
                )
            """

        return time("hent_soknader_for_formidler") {
            using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        statement,
                        mapOf(
                            "fnrInnsender" to fnrFormidler,
                            "soknadId" to soknadId
                        )
                    ).map {
                        SoknadForFormidler(
                            søknadId = UUID.fromString(it.string("SOKNADS_ID")),
                            status = Status.valueOf(it.string("STATUS")),
                            fullmakt = it.boolean("fullmakt"),
                            datoOpprettet = it.sqlTimestamp("created"),
                            datoOppdatert = when {
                                it.sqlTimestampOrNull("updated") != null -> it.sqlTimestamp("updated")
                                else -> it.sqlTimestamp("created")
                            },
                            fnrBruker = it.string("FNR_BRUKER"),
                            navnBruker = it.stringOrNull("NAVN_BRUKER"),
                            søknadsdata = Søknadsdata(
                                objectMapper.readTree(
                                    it.string("DATA")
                                ),
                                null
                            ),
                        )
                    }.asSingle
                )
            }
        }
    }
}

private inline fun <T : Any?> time(queryName: String, function: () -> T) =
    Prometheus.dbTimer.labels(queryName).startTimer().let { timer ->
        function().also {
            timer.observeDuration()
        }
    }

class SoknadForFormidler constructor(
    val søknadId: UUID,
    val datoOpprettet: Date,
    var datoOppdatert: Date,
    val status: Status,
    val fullmakt: Boolean,
    val fnrBruker: String,
    val navnBruker: String?,
    val søknadsdata: Søknadsdata? = null
)
