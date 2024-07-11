package no.nav.hjelpemidler.soknad.db.store

import com.fasterxml.jackson.databind.JsonNode
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.enum
import no.nav.hjelpemidler.database.json
import no.nav.hjelpemidler.database.jsonOrNull
import no.nav.hjelpemidler.database.sql.Sql
import no.nav.hjelpemidler.soknad.db.domain.BehovsmeldingType
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.domain.Søknadsdata
import no.nav.hjelpemidler.soknad.db.domain.behovsmeldingType
import no.nav.hjelpemidler.soknad.db.rolle.InnsenderRolle
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

private const val UKER_TILGJENGELIG_ETTER_ENDELIG_STATUS = 4

class SøknadStoreInnsender(private val tx: JdbcOperations) {
    fun hentSøknaderForInnsender(
        fnrInnsender: String,
        innsenderRolle: InnsenderRolle?,
        ukerEtterEndeligStatus: Int = UKER_TILGJENGELIG_ETTER_ENDELIG_STATUS,
    ): List<SøknadForInnsender> {
        val behovsmeldingTypeClause = when (innsenderRolle) {
            InnsenderRolle.BESTILLER -> "AND soknad.DATA ->> 'behovsmeldingType' = 'BESTILLING'"
            else -> ""
        }

        val statement = Sql(
            """
                SELECT soknad.SOKNADS_ID, soknad.DATA ->> 'behovsmeldingType' AS behovsmeldingType, soknad.CREATED, 
                soknad.UPDATED, soknad.DATA, soknad.FNR_BRUKER, soknad.NAVN_BRUKER, status.STATUS, status.ARSAKER,
                (CASE WHEN EXISTS (
                    SELECT 1 FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID AND STATUS IN ('GODKJENT_MED_FULLMAKT')
                ) THEN true ELSE false END) as fullmakt
                FROM V1_SOKNAD AS soknad
                LEFT JOIN V1_STATUS AS status
                ON status.ID = (
                    SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID ORDER BY created DESC LIMIT 1
                )
                WHERE soknad.FNR_INNSENDER = :fnrInnsender $behovsmeldingTypeClause
                AND soknad.created > :opprettetEtter
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
            """.trimIndent(),
        )

        return tx.list(
            statement,
            mapOf(
                "fnrInnsender" to fnrInnsender,
                "opprettetEtter" to LocalDateTime.now().minusMonths(6),
            ),
        ) {
            val datoOpprettet = it.sqlTimestamp("created")
            SøknadForInnsender(
                søknadId = it.uuid("soknads_id"),
                behovsmeldingType = it.behovsmeldingType("behovsmeldingType"),
                datoOpprettet = datoOpprettet,
                datoOppdatert = it.sqlTimestampOrNull("updated") ?: datoOpprettet,
                status = it.enum<Status>("status"),
                fullmakt = it.boolean("fullmakt"),
                fnrBruker = it.string("fnr_bruker"),
                navnBruker = it.stringOrNull("navn_bruker"),
                valgteÅrsaker = it.jsonOrNull<List<String>?>("arsaker") ?: emptyList(),
            )
        }
    }

    fun hentSøknadForInnsender(
        fnrInnsender: String,
        søknadId: UUID,
        innsenderRolle: InnsenderRolle?,
        ukerEtterEndeligStatus: Int = UKER_TILGJENGELIG_ETTER_ENDELIG_STATUS,
    ): SøknadForInnsender? {
        val behovsmeldingTypeClause = when (innsenderRolle) {
            InnsenderRolle.BESTILLER -> "AND soknad.DATA ->> 'behovsmeldingType' = 'BESTILLING'"
            else -> ""
        }

        val statement = Sql(
            """
                SELECT soknad.SOKNADS_ID, soknad.DATA ->> 'behovsmeldingType' AS behovsmeldingType, soknad.CREATED, soknad.UPDATED, soknad.DATA, soknad.FNR_BRUKER, soknad.NAVN_BRUKER, status.STATUS, status.ARSAKER,  
                (CASE WHEN EXISTS (
                    SELECT 1 FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID AND STATUS IN ('GODKJENT_MED_FULLMAKT')
                ) THEN true ELSE false END) as fullmakt
                FROM V1_SOKNAD AS soknad
                LEFT JOIN V1_STATUS AS status
                ON status.ID = (
                    SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID ORDER BY created DESC LIMIT 1
                )
                WHERE soknad.FNR_INNSENDER = :fnrInnsender 
                AND soknad.DATA ->> 'behovsmeldingType' <> 'BRUKERPASSBYTTE'
                AND soknad.SOKNADS_ID = :soknadId $behovsmeldingTypeClause
                AND soknad.created > :opprettetEtter
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
            """.trimIndent(),
        )

        return tx.singleOrNull(
            statement,
            mapOf(
                "fnrInnsender" to fnrInnsender,
                "soknadId" to søknadId,
                "opprettetEtter" to LocalDateTime.now().minusMonths(6),
            ),
        ) {
            val datoOpprettet = it.sqlTimestamp("created")
            SøknadForInnsender(
                søknadId = it.uuid("soknads_id"),
                behovsmeldingType = it.behovsmeldingType("behovsmeldingType"),
                datoOpprettet = datoOpprettet,
                datoOppdatert = it.sqlTimestampOrNull("updated") ?: datoOpprettet,
                status = it.enum<Status>("status"),
                fullmakt = it.boolean("fullmakt"),
                fnrBruker = it.string("fnr_bruker"),
                navnBruker = it.stringOrNull("navn_bruker"),
                søknadsdata = Søknadsdata(it.json<JsonNode>("data"), null),
                valgteÅrsaker = it.jsonOrNull<List<String>?>("arsaker") ?: emptyList(),
            )
        }
    }
}

class SøknadForInnsender(
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
