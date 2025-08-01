package no.nav.hjelpemidler.soknad.db.store

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Innsenderbehovsmelding
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.Store
import no.nav.hjelpemidler.database.sql.Sql
import no.nav.hjelpemidler.soknad.db.rolle.InnsenderRolle
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

private const val UKER_TILGJENGELIG_ETTER_ENDELIG_STATUS = 4

private val logg = KotlinLogging.logger {}

class SøknadStoreInnsender(private val tx: JdbcOperations) : Store {
    fun hentSøknaderForInnsender(
        fnrInnsender: String,
        innsenderRolle: InnsenderRolle?,
        ukerEtterEndeligStatus: Int = UKER_TILGJENGELIG_ETTER_ENDELIG_STATUS,
    ): List<SøknadForInnsender> {
        val behovsmeldingTypeClause = when (innsenderRolle) {
            InnsenderRolle.BESTILLER -> "AND soknad.data_v2 ->> 'type' = 'BESTILLING'"
            else -> ""
        }

        val statement = Sql(
            """
                SELECT soknad.soknads_id,
                       soknad.data_v2 ->> 'type' AS behovsmeldingtype,
                       soknad.created,
                       soknad.updated,
                       soknad.fnr_bruker,
                       soknad.navn_bruker,
                       status.status,
                       status.arsaker,
                       soknad.soknad_gjelder,
                       (CASE
                            WHEN EXISTS (SELECT 1
                                         FROM v1_status
                                         WHERE soknads_id = soknad.soknads_id AND status IN ('GODKJENT_MED_FULLMAKT')) THEN TRUE
                            ELSE FALSE END)                AS fullmakt
                FROM v1_soknad AS soknad
                         LEFT JOIN v1_status AS status
                                   ON status.id =
                                      (SELECT id FROM v1_status WHERE soknads_id = soknad.soknads_id ORDER BY created DESC LIMIT 1)
                WHERE TRUE
                  AND soknad.fnr_innsender = :fnrInnsender
                  $behovsmeldingTypeClause
                  AND soknad.created > :opprettetEtter
                     AND (
                      status.STATUS NOT IN ('SLETTET', 'UTLØPT', 'VEDTAKSRESULTAT_AVSLÅTT', 'VEDTAKSRESULTAT_HENLAGTBORTFALT', 'VEDTAKSRESULTAT_ANNET', 'BESTILLING_AVVIST', 'UTSENDING_STARTET')
                      OR (status.CREATED + INTERVAL '$ukerEtterEndeligStatus week') > now()
                  )
                  AND NOT (
                      -- Hvis vi har stått fast i status positivt vedtak i fire uker, og vedtaket kom før de siste fiksene våre så fjerner vi de fra formidleroversikten.
                      -- Dette trengs pga. hvordan vi har kastet ordrelinjer som ikke kunne knyttes til sak, og da ble man hengende igjen for alltid.
                      status.STATUS IN ('VEDTAKSRESULTAT_INNVILGET', 'VEDTAKSRESULTAT_MUNTLIG_INNVILGET', 'VEDTAKSRESULTAT_DELVIS_INNVILGET', 'BESTILLING_FERDIGSTILT')
                      AND status.CREATED < '2022-02-14' -- Dagen etter vi lanserte de siste fiksene
                      AND status.CREATED < (now() - INTERVAL '4 week') -- Vises i maks fire uker etter vedtak
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
                behovsmeldingType = it.tilBehovsmeldingType("behovsmeldingType"),
                datoOpprettet = datoOpprettet,
                datoOppdatert = it.sqlTimestampOrNull("updated") ?: datoOpprettet,
                status = it.enum<BehovsmeldingStatus>("status"),
                fullmakt = it.boolean("fullmakt"),
                fnrBruker = it.string("fnr_bruker"),
                navnBruker = it.stringOrNull("navn_bruker"),
                valgteÅrsaker = it.jsonOrNull<List<String>?>("arsaker") ?: emptyList(),
                soknadGjelder = it.stringOrNull("soknad_gjelder"),
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
            InnsenderRolle.BESTILLER -> "AND soknad.data_v2 ->> 'type' = 'BESTILLING'"
            else -> ""
        }

        val statement = Sql(
            """
                SELECT soknad.soknads_id,
                       soknad.data_v2 ->> 'type' AS behovsmeldingtype,
                       soknad.created,
                       soknad.updated,
                       soknad.data_v2,
                       soknad.fnr_bruker,
                       soknad.navn_bruker,
                       status.status,
                       status.arsaker,
                       soknad.soknad_gjelder,
                       (CASE
                            WHEN EXISTS (SELECT 1
                                         FROM v1_status
                                         WHERE soknads_id = soknad.soknads_id AND status IN ('GODKJENT_MED_FULLMAKT')) THEN TRUE
                            ELSE FALSE END)                AS fullmakt
                FROM v1_soknad AS soknad
                         LEFT JOIN v1_status AS status
                                   ON status.id =
                                      (SELECT id FROM v1_status WHERE soknads_id = soknad.soknads_id ORDER BY created DESC LIMIT 1)
                WHERE TRUE
                  AND soknad.fnr_innsender = :fnrInnsender
                  AND soknad.data_v2 ->> 'type' <> 'BRUKERPASSBYTTE'
                  AND soknad.soknads_id = :soknadId
                  $behovsmeldingTypeClause
                  AND soknad.created > :opprettetEtter
                     AND (
                      status.STATUS NOT IN ('SLETTET', 'UTLØPT', 'VEDTAKSRESULTAT_AVSLÅTT', 'VEDTAKSRESULTAT_HENLAGTBORTFALT', 'VEDTAKSRESULTAT_ANNET', 'BESTILLING_AVVIST', 'UTSENDING_STARTET')
                      OR (status.CREATED + INTERVAL '$ukerEtterEndeligStatus week') > now()
                  )
                  AND NOT (
                      -- Hvis vi har stått fast i status positivt vedtak i fire uker, og vedtaket kom før de siste fiksene våre så fjerner vi de fra formidleroversikten.
                      -- Dette trengs pga. hvordan vi har kastet ordrelinjer som ikke kunne knyttes til sak, og da ble man hengende igjen for alltid.
                      status.STATUS IN ('VEDTAKSRESULTAT_INNVILGET', 'VEDTAKSRESULTAT_MUNTLIG_INNVILGET', 'VEDTAKSRESULTAT_DELVIS_INNVILGET', 'BESTILLING_FERDIGSTILT')
                      AND status.CREATED < '2022-02-14' -- Dagen etter vi lanserte de siste fiksene
                      AND status.CREATED < (now() - INTERVAL '4 week') -- Vises i maks fire uker etter vedtak
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
                behovsmeldingType = it.tilBehovsmeldingType("behovsmeldingType"),
                datoOpprettet = datoOpprettet,
                datoOppdatert = it.sqlTimestampOrNull("updated") ?: datoOpprettet,
                status = it.enum<BehovsmeldingStatus>("status"),
                fullmakt = it.boolean("fullmakt"),
                fnrBruker = it.string("fnr_bruker"),
                navnBruker = it.stringOrNull("navn_bruker"),
                valgteÅrsaker = it.jsonOrNull<List<String>?>("arsaker") ?: emptyList(),
                behovsmelding = it.tilInnsenderbehovsmelding(),
                soknadGjelder = it.stringOrNull("soknad_gjelder"),
            )
        }
    }
}

class SøknadForInnsender(
    val søknadId: UUID,
    val behovsmeldingType: BehovsmeldingType,
    val datoOpprettet: Date,
    var datoOppdatert: Date,
    val status: BehovsmeldingStatus,
    val fullmakt: Boolean,
    val fnrBruker: String,
    val navnBruker: String?,
    val valgteÅrsaker: List<String>,
    val behovsmelding: Innsenderbehovsmelding? = null,
    val soknadGjelder: String?,
)
