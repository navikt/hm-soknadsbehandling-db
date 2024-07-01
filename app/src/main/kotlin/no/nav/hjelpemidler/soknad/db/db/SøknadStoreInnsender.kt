package no.nav.hjelpemidler.soknad.db.db

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.hjelpemidler.soknad.db.domain.BehovsmeldingType
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.domain.Søknadsdata
import no.nav.hjelpemidler.soknad.db.rolle.InnsenderRolle
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID
import javax.sql.DataSource

private const val UKER_TILGJENGELIG_ETTER_ENDELIG_STATUS = 4

internal interface SøknadStoreInnsender {
    fun hentSøknaderForInnsender(
        fnrInnsender: String,
        innsenderRolle: InnsenderRolle?,
        ukerEtterEndeligStatus: Int = UKER_TILGJENGELIG_ETTER_ENDELIG_STATUS,
    ): List<SoknadForInnsender>

    fun hentSøknadForInnsender(
        fnrInnsender: String,
        soknadId: UUID,
        innsenderRolle: InnsenderRolle?,
        ukerEtterEndeligStatus: Int = UKER_TILGJENGELIG_ETTER_ENDELIG_STATUS,
    ): SoknadForInnsender?
}

internal class SøknadStoreInnsenderPostgres(private val dataSource: DataSource) : SøknadStoreInnsender {

    override fun hentSøknaderForInnsender(
        fnrInnsender: String,
        innsenderRolle: InnsenderRolle?,
        ukerEtterEndeligStatus: Int,
    ): List<SoknadForInnsender> {
        val behovsmeldingTypeClause = when (innsenderRolle) {
            InnsenderRolle.BESTILLER -> "AND soknad.DATA ->> 'behovsmeldingType' = 'BESTILLING' "
            else -> ""
        }

        @Language("PostgreSQL")
        val statement =
            """
                SELECT soknad.SOKNADS_ID, soknad.DATA ->> 'behovsmeldingType' AS behovsmeldingType, soknad.CREATED, 
                soknad.UPDATED, soknad.DATA, soknad.FNR_BRUKER, soknad.NAVN_BRUKER, status.STATUS, status.ARSAKER,
                (CASE WHEN EXISTS (
                    SELECT 1 FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID AND STATUS IN  ('GODKJENT_MED_FULLMAKT')
                ) THEN true ELSE false END) as fullmakt
                FROM V1_SOKNAD AS soknad
                LEFT JOIN V1_STATUS AS status
                ON status.ID = (
                    SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID ORDER BY created DESC LIMIT 1
                )
                WHERE soknad.FNR_INNSENDER = ? $behovsmeldingTypeClause
                AND soknad.created > ?
                AND (
                    status.STATUS NOT IN ('SLETTET', 'UTLØPT', 'VEDTAKSRESULTAT_AVSLÅTT', 'VEDTAKSRESULTAT_HENLAGTBORTFALT', 'VEDTAKSRESULTAT_ANNET', 'BESTILLING_AVVIST', 'UTSENDING_STARTET')
                    OR (status.CREATED + interval '$ukerEtterEndeligStatus week') > now()
                )
                AND NOT (
                    -- Hvis vi har stått fast i status positivt vedtak i fire uker, og vedtaket kom før de siste fiksene våre så fjerner vi de fra formidleroversikten.
                    -- Dette trengs pga. hvordan vi har kastet ordrelinjer som ikke kunne knyttes til sak, og da ble man hengende igjen for alltid.
                    status.STATUS IN ('VEDTAKSRESULTAT_INNVILGET', 'VEDTAKSRESULTAT_MUNTLIG_INNVILGET', 'VEDTAKSRESULTAT_DELVIS_INNVILGET', 'BESTILLING_FERDIGSTILT')
                    AND status.CREATED < '2022-02-14' -- Dagen etter vi lanserte de siste fiksene
                    AND status.CREATED < (now() - interval '4 week') -- Vises i maks fire uker etter vedtak
                )
                ORDER BY soknad.UPDATED DESC
            """

        return time("hent_soknader_for_innsender") {
            using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        statement,
                        fnrInnsender,
                        LocalDateTime.now().minusMonths(6),
                    ).map {
                        SoknadForInnsender(
                            søknadId = UUID.fromString(it.string("SOKNADS_ID")),
                            behovsmeldingType = BehovsmeldingType.valueOf(
                                it.stringOrNull("behovsmeldingType").let { it ?: "SØKNAD" },
                            ),
                            status = Status.valueOf(it.string("STATUS")),
                            valgteÅrsaker = objectMapper.readValue(
                                it.stringOrNull("ARSAKER") ?: "[]",
                            ),
                            fullmakt = it.boolean("fullmakt"),
                            datoOpprettet = it.sqlTimestamp("created"),
                            datoOppdatert = when {
                                it.sqlTimestampOrNull("updated") != null -> it.sqlTimestamp("updated")
                                else -> it.sqlTimestamp("created")
                            },
                            fnrBruker = it.string("FNR_BRUKER"),
                            navnBruker = it.stringOrNull("NAVN_BRUKER"),
                        )
                    }.asList,
                )
            }
        }
    }

    override fun hentSøknadForInnsender(
        fnrInnsender: String,
        soknadId: UUID,
        innsenderRolle: InnsenderRolle?,
        ukerEtterEndeligStatus: Int,
    ): SoknadForInnsender? {
        val behovsmeldingTypeClause = when (innsenderRolle) {
            InnsenderRolle.BESTILLER -> "AND soknad.DATA ->> 'behovsmeldingType' = 'BESTILLING' "
            else -> ""
        }

        @Language("PostgreSQL")
        val statement =
            """
                SELECT soknad.SOKNADS_ID, soknad.DATA ->> 'behovsmeldingType' AS behovsmeldingType, soknad.CREATED, soknad.UPDATED, soknad.DATA, soknad.FNR_BRUKER, soknad.NAVN_BRUKER, status.STATUS, status.ARSAKER,  
                (CASE WHEN EXISTS (
                    SELECT 1 FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID AND STATUS IN  ('GODKJENT_MED_FULLMAKT')
                ) THEN true ELSE false END) as fullmakt
                FROM V1_SOKNAD AS soknad
                LEFT JOIN V1_STATUS AS status
                ON status.ID = (
                    SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID ORDER BY created DESC LIMIT 1
                )
                WHERE soknad.FNR_INNSENDER = :fnrInnsender 
                AND soknad.DATA ->> 'behovsmeldingType' <> 'BRUKERPASSBYTTE'
                AND soknad.SOKNADS_ID = :soknadId $behovsmeldingTypeClause
                AND soknad.created > :minimumDato
                AND (
                    status.STATUS NOT IN ('SLETTET', 'UTLØPT', 'VEDTAKSRESULTAT_AVSLÅTT', 'VEDTAKSRESULTAT_HENLAGTBORTFALT', 'VEDTAKSRESULTAT_ANNET', 'BESTILLING_AVVIST', 'UTSENDING_STARTET')
                    OR (status.CREATED + interval '$ukerEtterEndeligStatus week') > now()
                )
                AND NOT (
                    -- Hvis vi har stått fast i status positivt vedtak i fire uker, og vedtaket kom før de siste fiksene våre så fjerner vi de fra formidleroversikten.
                    -- Dette trengs pga. hvordan vi har kastet ordrelinjer som ikke kunne knyttes til sak, og da ble man hengende igjen for alltid.
                    status.STATUS IN ('VEDTAKSRESULTAT_INNVILGET', 'VEDTAKSRESULTAT_MUNTLIG_INNVILGET', 'VEDTAKSRESULTAT_DELVIS_INNVILGET', 'BESTILLING_FERDIGSTILT')
                    AND status.CREATED < '2022-02-14' -- Dagen etter vi lanserte de siste fiksene
                    AND status.CREATED < (now() - interval '4 week') -- Vises i maks fire uker etter vedtak
                )
            """

        return time("hent_soknad_for_innsender") {
            using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        statement,
                        mapOf(
                            "fnrInnsender" to fnrInnsender,
                            "soknadId" to soknadId,
                            "minimumDato" to LocalDateTime.now().minusMonths(6),
                        ),
                    ).map {
                        SoknadForInnsender(
                            søknadId = UUID.fromString(it.string("SOKNADS_ID")),
                            behovsmeldingType = BehovsmeldingType.valueOf(
                                it.stringOrNull("behovsmeldingType").let { it ?: "SØKNAD" },
                            ),
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
                                    it.string("DATA"),
                                ),
                                null,
                            ),
                            valgteÅrsaker = objectMapper.readValue(
                                it.stringOrNull("ARSAKER") ?: "[]",
                            ),
                        )
                    }.asSingle,
                )
            }
        }
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }
}

class SoknadForInnsender(
    val søknadId: UUID,
    val behovsmeldingType: BehovsmeldingType,
    val datoOpprettet: Date,
    var datoOppdatert: Date,
    val status: Status,
    val fullmakt: Boolean,
    val fnrBruker: String,
    val navnBruker: String?,
    val søknadsdata: Søknadsdata? = null,
    val valgteÅrsaker: List<String>,
)
