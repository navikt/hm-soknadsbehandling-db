package no.nav.hjelpemidler.soknad.db.store

import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.engine.apache.Apache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.hjelpemidler.collections.enumSetOf
import no.nav.hjelpemidler.collections.toStringArray
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.enum
import no.nav.hjelpemidler.database.enumOrNull
import no.nav.hjelpemidler.database.json
import no.nav.hjelpemidler.database.jsonOrNull
import no.nav.hjelpemidler.database.pgJsonbOf
import no.nav.hjelpemidler.database.sql.Sql
import no.nav.hjelpemidler.http.slack.slack
import no.nav.hjelpemidler.http.slack.slackIconEmoji
import no.nav.hjelpemidler.soknad.db.domain.BehovsmeldingType
import no.nav.hjelpemidler.soknad.db.domain.ForslagsmotorTilbehørHjelpemiddelListe
import no.nav.hjelpemidler.soknad.db.domain.ForslagsmotorTilbehørHjelpemidler
import no.nav.hjelpemidler.soknad.db.domain.ForslagsmotorTilbehørSøknad
import no.nav.hjelpemidler.soknad.db.domain.PapirSøknadData
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.domain.StatusCountRow
import no.nav.hjelpemidler.soknad.db.domain.StatusMedÅrsak
import no.nav.hjelpemidler.soknad.db.domain.StatusRow
import no.nav.hjelpemidler.soknad.db.domain.SøknadData
import no.nav.hjelpemidler.soknad.db.domain.SøknadForBruker
import no.nav.hjelpemidler.soknad.db.domain.SøknadMedStatus
import no.nav.hjelpemidler.soknad.db.domain.UtgåttSøknad
import no.nav.hjelpemidler.soknad.db.domain.behovsmeldingType
import no.nav.hjelpemidler.soknad.db.domain.kommuneapi.Behovsmelding
import no.nav.hjelpemidler.soknad.db.domain.kommuneapi.SøknadForKommuneApi
import no.nav.hjelpemidler.soknad.db.jsonMapper
import java.math.BigInteger
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID

private val logg = KotlinLogging.logger {}

class SøknadStore(private val tx: JdbcOperations) {
    private val slack = slack(engine = Apache.create())

    fun søknadFinnes(søknadId: UUID): Boolean {
        val uuid = tx.singleOrNull(
            """
                SELECT soknads_id
                FROM v1_soknad
                WHERE soknads_id = :soknadId
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) { it.uuid("soknads_id") }
        return uuid != null
    }

    fun fnrOgJournalpostIdFinnes(fnrBruker: String, journalpostId: Int): Boolean {
        val uuid = tx.singleOrNull(
            """
                SELECT soknads_id
                FROM v1_soknad
                WHERE fnr_bruker = :fnrBruker
                  AND journalpostid = :journalpostId
            """.trimIndent(),
            mapOf(
                "fnrBruker" to fnrBruker,
                "journalpostId" to journalpostId,
            ),
        ) { it.uuid("soknads_id") }
        return uuid != null
    }

    fun hentSoknad(søknadId: UUID): SøknadForBruker? {
        val statement = Sql(
            """
                SELECT soknad.soknads_id, soknad.data ->> 'behovsmeldingType' AS behovsmeldingtype, soknad.journalpostid, soknad.data, soknad.created, soknad.kommunenavn, soknad.fnr_bruker, soknad.updated, soknad.er_digital, soknad.soknad_gjelder, status.status, status.arsaker, 
                (CASE WHEN EXISTS (
                    SELECT 1 FROM v1_status WHERE soknads_id = soknad.soknads_id AND status IN ('GODKJENT_MED_FULLMAKT')
                ) THEN TRUE ELSE FALSE END) AS fullmakt
                FROM v1_soknad AS soknad 
                LEFT JOIN v1_status AS status
                ON status.id = (
                    SELECT id FROM v1_status WHERE soknads_id = soknad.soknads_id ORDER BY created DESC LIMIT 1
                )
                WHERE soknad.soknads_id = :soknadId
                  AND NOT (status.status = ANY (:status))
            """.trimIndent(),
        )

        return tx.singleOrNull(
            statement,
            mapOf(
                "soknadId" to søknadId,
                "status" to enumSetOf(
                    Status.GODKJENT_MED_FULLMAKT,
                    Status.INNSENDT_FULLMAKT_IKKE_PÅKREVD,
                ).toStringArray(),
            ),
        ) {
            val status = it.enum<Status>("status")
            val datoOpprettet = it.sqlTimestamp("created")
            val datoOppdatert = it.sqlTimestampOrNull("updated") ?: datoOpprettet
            if (status.isSlettetEllerUtløpt() || !it.boolean("ER_DIGITAL")) {
                SøknadForBruker.newEmptySøknad(
                    søknadId = it.uuid("SOKNADS_ID"),
                    behovsmeldingType = it.behovsmeldingType("behovsmeldingType"),
                    journalpostId = it.stringOrNull("JOURNALPOSTID"),
                    status = status,
                    fullmakt = it.boolean("fullmakt"),
                    datoOpprettet = datoOpprettet,
                    datoOppdatert = datoOppdatert,
                    fnrBruker = it.string("FNR_BRUKER"),
                    er_digital = it.boolean("ER_DIGITAL"),
                    soknadGjelder = it.stringOrNull("SOKNAD_GJELDER"),
                    ordrelinjer = emptyList(),
                    fagsakId = null,
                    søknadType = null,
                    valgteÅrsaker = it.jsonOrNull<List<String>>("ARSAKER") ?: emptyList(),
                )
            } else {
                SøknadForBruker.new(
                    søknadId = it.uuid("SOKNADS_ID"),
                    behovsmeldingType = it.behovsmeldingType("behovsmeldingType"),
                    journalpostId = it.stringOrNull("JOURNALPOSTID"),
                    status = status,
                    fullmakt = it.boolean("fullmakt"),
                    datoOpprettet = datoOpprettet,
                    datoOppdatert = datoOppdatert,
                    søknad = it.jsonOrNull<JsonNode>("DATA") ?: jsonMapper.createObjectNode(),
                    kommunenavn = it.stringOrNull("KOMMUNENAVN"),
                    fnrBruker = it.string("FNR_BRUKER"),
                    er_digital = it.boolean("ER_DIGITAL"),
                    soknadGjelder = it.stringOrNull("SOKNAD_GJELDER"),
                    ordrelinjer = emptyList(),
                    fagsakId = null,
                    søknadType = null,
                    valgteÅrsaker = it.jsonOrNull<List<String>>("ARSAKER") ?: emptyList(),
                )
            }
        }
    }

    fun hentSøknadOpprettetDato(søknadId: UUID): Date? {
        return tx.singleOrNull(
            """
                SELECT created
                FROM v1_soknad
                WHERE soknads_id = :soknadId
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) { it.sqlTimestamp("created") }
    }

    fun hentSøknadData(søknadId: UUID): SøknadData? {
        val statement = Sql(
            """
                SELECT soknad.soknads_id, soknad.fnr_bruker, soknad.navn_bruker, soknad.fnr_innsender, soknad.data, soknad.kommunenavn, soknad.er_digital, soknad.soknad_gjelder, status.status
                FROM v1_soknad AS soknad
                LEFT JOIN v1_status AS status
                ON status.id = (
                    SELECT id FROM v1_status WHERE soknads_id = soknad.soknads_id ORDER BY created DESC LIMIT 1
                )
                WHERE soknad.soknads_id = :soknadId
            """.trimIndent(),
        )

        return tx.singleOrNull(
            statement,
            mapOf("soknadId" to søknadId),
        ) {
            SøknadData(
                fnrBruker = it.string("FNR_BRUKER"),
                navnBruker = it.string("NAVN_BRUKER"),
                fnrInnsender = it.stringOrNull("FNR_INNSENDER"),
                soknadId = it.uuid("SOKNADS_ID"),
                status = Status.valueOf(it.string("STATUS")),
                soknad = it.jsonOrNull<JsonNode>("DATA") ?: jsonMapper.createObjectNode(),
                kommunenavn = it.stringOrNull("KOMMUNENAVN"),
                er_digital = it.boolean("ER_DIGITAL"),
                soknadGjelder = it.stringOrNull("SOKNAD_GJELDER"),
            )
        }
    }

    fun hentFnrForSøknad(søknadId: UUID): String {
        return tx.singleOrNull(
            """
                SELECT fnr_bruker
                FROM v1_soknad
                WHERE soknads_id = :soknadId
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) { it.string("fnr_bruker") } ?: error("Fant ikke fnr for søknadId: $søknadId")
    }

    fun oppdaterStatusMedÅrsak(statusMedÅrsak: StatusMedÅrsak): Int {
        if (checkIfLastStatusMatches(statusMedÅrsak.søknadId, statusMedÅrsak.status)) return 0
        tx.update(
            """
                INSERT INTO v1_status (soknads_id, status, begrunnelse, arsaker)
                VALUES (:soknadId, :status, :begrunnelse, :arsaker)
            """.trimIndent(),
            mapOf(
                "soknadId" to statusMedÅrsak.søknadId,
                "status" to statusMedÅrsak.status,
                "begrunnelse" to statusMedÅrsak.begrunnelse,
                "arsaker" to statusMedÅrsak.valgteÅrsaker?.let { pgJsonbOf(it) },
            ),
        )
        return tx.update(
            "UPDATE v1_soknad SET updated = NOW() WHERE soknads_id = :soknadId",
            mapOf("soknadId" to statusMedÅrsak.søknadId),
        ).actualRowCount
    }

    fun oppdaterStatus(søknadId: UUID, status: Status): Int {
        if (checkIfLastStatusMatches(søknadId, status)) return 0
        tx.update(
            "INSERT INTO v1_status (soknads_id, status) VALUES (:soknadId, :status)",
            mapOf(
                "soknadId" to søknadId,
                "status" to status,
            ),
        )
        return tx.update(
            "UPDATE v1_soknad SET updated = NOW() WHERE soknads_id = :soknadId",
            mapOf("soknadId" to søknadId),
        ).actualRowCount
    }

    fun slettSøknad(søknadId: UUID): Int = slettSøknad(søknadId, Status.SLETTET)

    fun slettUtløptSøknad(søknadId: UUID): Int = slettSøknad(søknadId, Status.UTLØPT)

    private fun slettSøknad(søknadId: UUID, status: Status): Int {
        if (checkIfLastStatusMatches(søknadId, status)) return 0
        tx.update(
            "INSERT INTO v1_status (soknads_id, status) VALUES (:soknadId, :status)",
            mapOf(
                "soknadId" to søknadId,
                "status" to status,
            ),
        )
        return tx.update(
            "UPDATE v1_soknad SET updated = NOW(), data = NULL WHERE soknads_id = :soknadId",
            mapOf("soknadId" to søknadId),
        ).actualRowCount
    }

    fun oppdaterJournalpostId(søknadId: UUID, journalpostId: String): Int {
        return tx.update(
            """
                UPDATE v1_soknad 
                SET journalpostid = :journalpostId,
                    updated = NOW() 
                WHERE soknads_id = :soknadId
            """.trimIndent(),
            mapOf(
                "journalpostId" to BigInteger(journalpostId),
                "soknadId" to søknadId,
            ),
        ).actualRowCount
    }

    fun oppdaterOppgaveId(søknadId: UUID, oppgaveId: String): Int {
        return tx.update(
            """
                UPDATE v1_soknad
                SET oppgaveid = :oppgaveId,
                    updated = NOW()
                WHERE soknads_id = :soknadId
                  AND oppgaveid IS NULL
            """.trimIndent(),
            mapOf(
                "oppgaveId" to BigInteger(oppgaveId),
                "soknadId" to søknadId,
            ),
        ).actualRowCount
    }

    fun hentSoknaderForBruker(fnrBruker: String): List<SøknadMedStatus> {
        val statement = Sql(
            """
                SELECT soknad.soknads_id, soknad.data ->> 'behovsmeldingType' AS behovsmeldingtype, soknad.journalpostid, soknad.created, soknad.updated, soknad.data, soknad.er_digital, soknad.soknad_gjelder, status.status, status.arsaker,
                (CASE WHEN EXISTS (
                    SELECT 1 FROM v1_status WHERE soknads_id = soknad.soknads_id AND status IN ('GODKJENT_MED_FULLMAKT')
                ) THEN TRUE ELSE FALSE END) AS fullmakt
                FROM v1_soknad AS soknad
                LEFT JOIN v1_status AS status
                ON status.id = (
                    SELECT id FROM v1_status WHERE soknads_id = soknad.soknads_id ORDER BY created DESC LIMIT 1
                )
                WHERE soknad.fnr_bruker = :fnrBruker
                  AND NOT(status.status = ANY (:status))
                ORDER BY soknad.created DESC
            """.trimIndent(),
        )

        return tx.list(
            statement,
            mapOf(
                "fnrBruker" to fnrBruker,
                "status" to enumSetOf(
                    Status.GODKJENT_MED_FULLMAKT,
                    Status.INNSENDT_FULLMAKT_IKKE_PÅKREVD,
                ).toStringArray(),
            ),
        ) {
            val status = it.enum<Status>("STATUS")
            val datoOpprettet = it.sqlTimestamp("created")
            val datoOppdatert = it.sqlTimestampOrNull("updated") ?: datoOpprettet
            if (status.isSlettetEllerUtløpt() || !it.boolean("ER_DIGITAL")) {
                SøknadMedStatus.newSøknadUtenFormidlernavn(
                    soknadId = it.uuid("SOKNADS_ID"),
                    behovsmeldingType = it.behovsmeldingType("behovsmeldingType"),
                    journalpostId = it.stringOrNull("JOURNALPOSTID"),
                    status = status,
                    fullmakt = it.boolean("fullmakt"),
                    datoOpprettet = datoOpprettet,
                    datoOppdatert = datoOppdatert,
                    er_digital = it.boolean("ER_DIGITAL"),
                    soknadGjelder = it.stringOrNull("SOKNAD_GJELDER"),
                    valgteÅrsaker = it.jsonOrNull<List<String>?>("ARSAKER") ?: emptyList(),
                )
            } else {
                SøknadMedStatus.newSøknadMedFormidlernavn(
                    soknadId = it.uuid("SOKNADS_ID"),
                    behovsmeldingType = it.behovsmeldingType("behovsmeldingType"),
                    journalpostId = it.stringOrNull("JOURNALPOSTID"),
                    status = status,
                    fullmakt = it.boolean("fullmakt"),
                    datoOpprettet = datoOpprettet,
                    datoOppdatert = datoOppdatert,
                    søknad = it.jsonOrNull<JsonNode>("DATA") ?: jsonMapper.createObjectNode(),
                    er_digital = it.boolean("ER_DIGITAL"),
                    soknadGjelder = it.stringOrNull("SOKNAD_GJELDER"),
                    valgteÅrsaker = it.jsonOrNull<List<String>?>("ARSAKER") ?: emptyList(),
                )
            }
        }
    }

    fun hentSøknaderTilGodkjenningEldreEnn(dager: Int): List<UtgåttSøknad> {
        val statement = Sql(
            """
                SELECT soknad.SOKNADS_ID, soknad.FNR_BRUKER, status.STATUS
                FROM V1_SOKNAD AS soknad
                LEFT JOIN V1_STATUS AS status
                ON status.ID = (
                    SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID ORDER BY created DESC LIMIT 1
                )
                WHERE status.STATUS = :status
                  AND (soknad.CREATED + interval '$dager day') < now()
                ORDER BY soknad.CREATED DESC
            """.trimIndent(),
        )

        return tx.list(
            statement,
            mapOf("status" to Status.VENTER_GODKJENNING),
        ) {
            UtgåttSøknad(
                søknadId = it.uuid("SOKNADS_ID"),
                status = it.enum<Status>("STATUS"),
                fnrBruker = it.string("FNR_BRUKER"),
            )
        }
    }

    fun save(søknadData: SøknadData): Int {
        if (!checkIfLastStatusMatches(søknadData.soknadId, søknadData.status)) {
            tx.update(
                "INSERT INTO v1_status (soknads_id, status) VALUES (:soknadId, :status)",
                mapOf(
                    "soknadId" to søknadData.soknadId,
                    "status" to søknadData.status,
                ),
            )
        }
        return tx.update(
            """
                INSERT INTO v1_soknad (soknads_id, fnr_bruker, navn_bruker, fnr_innsender, data, kommunenavn, er_digital, soknad_gjelder)
                VALUES (:soknadId, :fnrBruker, :navnBruker, :fnrInnsender, :data, :kommunenavn, TRUE, :soknadGjelder)
                ON CONFLICT DO NOTHING
            """.trimIndent(),
            mapOf(
                "soknadId" to søknadData.soknadId,
                "fnrBruker" to søknadData.fnrBruker,
                "navnBruker" to søknadData.navnBruker,
                "fnrInnsender" to søknadData.fnrInnsender,
                "data" to pgJsonbOf(søknadData.soknad),
                "kommunenavn" to søknadData.kommunenavn,
                "soknadGjelder" to (søknadData.soknadGjelder ?: "Søknad om hjelpemidler"),
            ),
        ).actualRowCount
    }

    private fun checkIfLastStatusMatches(søknadId: UUID, status: Status): Boolean {
        return tx.singleOrNull(
            """
                SELECT status FROM v1_status
                WHERE id = (
                    SELECT id FROM v1_status
                    WHERE soknads_id = :soknadId
                    ORDER BY created DESC
                    LIMIT 1
                )
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) { it.enumOrNull<Status>("status") } == status
    }

    fun savePapir(soknadData: PapirSøknadData): Int {
        if (!checkIfLastStatusMatches(soknadData.soknadId, soknadData.status)) {
            tx.update(
                "INSERT INTO v1_status (soknads_id, status) VALUES (:soknadId, :status)",
                mapOf(
                    "soknadId" to soknadData.soknadId,
                    "status" to soknadData.status,
                ),
            )
        }
        return tx.update(
            """
                INSERT INTO v1_soknad (soknads_id, fnr_bruker, er_digital, journalpostid, navn_bruker)
                VALUES (:soknadId, :fnrBruker, FALSE, :journalpostId, :navnBruker)
                ON CONFLICT DO NOTHING
            """.trimIndent(),
            mapOf(
                "soknadId" to soknadData.soknadId,
                "fnrBruker" to soknadData.fnrBruker,
                "journalpostId" to soknadData.journalpostid,
                "navnBruker" to soknadData.navnBruker,
            ),
        ).actualRowCount
    }

    fun hentInitieltDatasettForForslagsmotorTilbehør(): List<ForslagsmotorTilbehørHjelpemidler> {
        val statement = Sql(
            """
                SELECT data, created FROM v1_soknad WHERE er_digital AND data IS NOT NULL
            """.trimIndent(),
        )

        val søknader = tx.list(statement) {
            val hjelpemiddel = it.json<ForslagsmotorTilbehørHjelpemidler>("data")
            hjelpemiddel.created = it.localDateTime("created")
            hjelpemiddel
        }

        // Filter out products with no accessories (ca. 2/3 of the cases)
        return søknader.map { soknad ->
            ForslagsmotorTilbehørHjelpemidler(
                soknad = ForslagsmotorTilbehørSøknad(
                    id = soknad.soknad.id,
                    hjelpemidler = ForslagsmotorTilbehørHjelpemiddelListe(
                        hjelpemiddelListe = soknad.soknad.hjelpemidler.hjelpemiddelListe.filter {
                            it.tilbehorListe?.isNotEmpty() ?: false
                        },
                    ),
                ),
                created = soknad.created,
            )
        }
    }

    fun hentGodkjenteBehovsmeldingerUtenOppgaveEldreEnn(dager: Int): List<String> {
        val statement = Sql(
            """
                WITH soknader_med_siste_status_godkjent AS (
                SELECT *
                FROM (SELECT soknads_id,
                             status,
                             RANK() OVER (PARTITION BY soknads_id ORDER BY created DESC) AS rangering
                      FROM V1_STATUS
                      WHERE created > NOW() - INTERVAL '90 DAYS') AS t
                WHERE rangering = 1
                  AND STATUS = ANY (:status)
                )

                SELECT soknad.soknads_id
                FROM V1_SOKNAD AS soknad
                         INNER JOIN soknader_med_siste_status_godkjent
                                    ON soknad.soknads_id = soknader_med_siste_status_godkjent.soknads_id
                WHERE soknad.oppgaveid IS NULL
                    AND soknad.CREATED < now() - INTERVAL '$dager DAYS' -- Buffer for saksbehanling etc.
                    AND soknad.created > now() - INTERVAL '90 DAYS' -- OPPGAVEID kolonnen ble lagt til 2021-04-12. Alt før dette har OPPGAVEID == NULL
            """.trimIndent(),
        )

        return tx.list(
            statement,
            mapOf(
                "status" to enumSetOf(
                    Status.GODKJENT_MED_FULLMAKT,
                    Status.GODKJENT,
                    Status.INNSENDT_FULLMAKT_IKKE_PÅKREVD,
                    Status.BRUKERPASSBYTTE_INNSENDT,
                ).toStringArray(),
            ),
        ) { it.string("soknads_id") }
    }

    fun behovsmeldingTypeFor(søknadId: UUID): BehovsmeldingType? {
        return tx.singleOrNull(
            """
                SELECT soknad.data ->> 'behovsmeldingType' AS behovsmeldingtype
                FROM v1_soknad AS soknad
                WHERE soknad.soknads_id = :soknadId
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) { it.behovsmeldingType("behovsmeldingType") }
    }

    fun tellStatuser(): List<StatusCountRow> {
        return tx.list(
            """
                WITH siste_status AS (SELECT soknads_id, status
                      FROM (SELECT soknads_id,
                                   status,
                                   RANK() OVER (PARTITION BY soknads_id ORDER BY created DESC) AS rangering
                            FROM v1_status) t
                      WHERE rangering = 1)

                SELECT status,
                       COUNT(soknads_id) AS count
                FROM siste_status
                GROUP BY status
            """.trimIndent(),
        ) {
            StatusCountRow(
                it.enum("STATUS"),
                it.int("COUNT"),
            )
        }
    }

    fun hentStatuser(søknadId: UUID): List<StatusRow> {
        return tx.list(
            """
                SELECT status, v1_status.created AS created, er_digital 
                FROM v1_status
                    JOIN v1_soknad ON v1_status.soknads_id = v1_soknad.soknads_id 
                WHERE v1_status.soknads_id = :soknadId
                ORDER BY created DESC
            """.trimIndent(), // ORDER is just a preventative measure
            mapOf("soknadId" to søknadId),
        ) {
            StatusRow(
                it.enum("STATUS"),
                it.sqlTimestamp("CREATED"),
                it.boolean("ER_DIGITAL"),
            )
        }
    }

    private var hentSoknaderForKommuneApietSistRapportertSlack = LocalDateTime.now().minusHours(2)
    fun hentSøknaderForKommuneApiet(
        kommunenummer: String,
        nyereEnn: UUID?,
        nyereEnnTidsstempel: Long?,
    ): List<SøknadForKommuneApi> {
        val extraWhere1 =
            if (nyereEnn == null) "" else "AND CREATED > (SELECT CREATED FROM V1_SOKNAD WHERE SOKNADS_ID = :nyereEnn)"
        val extraWhere2 = if (nyereEnnTidsstempel == null) "" else "AND CREATED > :nyereEnnTidsstempel"

        val statement = Sql(
            """
                SELECT
                    FNR_BRUKER,
                    NAVN_BRUKER,
                    FNR_INNSENDER,
                    SOKNADS_ID,
                    DATA,
                    SOKNAD_GJELDER,
                    CREATED
                FROM V1_SOKNAD
                WHERE
                    -- Sjekk at formidleren som sendte inn søknaden bor i kommunen som spør etter kvitteringer
                	DATA->'soknad'->'innsender'->'organisasjoner' @> :kommunenummerJson
                    -- Sjekk at brukeren det søkes om bor i samme kommune
                    AND DATA->'soknad'->'bruker'->>'kommunenummer' = :kommunenummer
                    -- Bare søknader/bestillinger sendt inn av formidlere kan kvitteres tilbake på dette tidspunktet
                    AND DATA->'soknad'->'innsender'->>'somRolle' = 'FORMIDLER'
                    -- Ikke gi tilgang til gamlere søknader enn 7 dager feks.
                    AND CREATED > NOW() - '7 days'::interval
                    -- Kun digitale søknader kan kvitteres tilbake til innsender kommunen
                    AND ER_DIGITAL
                    -- Videre filtrering basert på kommunens filtre
                    $extraWhere1
                    $extraWhere2
                ORDER BY CREATED ASC
            """.trimIndent(),
        )

        return tx.list(
            statement,
            mapOf(
                "kommunenummer" to kommunenummer,
                "kommunenummerJson" to pgJsonbOf(listOf(mapOf("kommunenummer" to kommunenummer))),
                "nyereEnn" to nyereEnn,
                "nyereEnnTidsstempel" to nyereEnnTidsstempel?.let { nyereEnnTidsstempel ->
                    LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(nyereEnnTidsstempel),
                        ZoneId.systemDefault(),
                    )
                },
            ),
        ) {
            val data = it.jsonOrNull<JsonNode>("DATA") ?: jsonMapper.createObjectNode()

            // Valider data-feltet, og hvis ikke filtrer ut raden ved å returnere null-verdi
            val validatedData = runCatching { Behovsmelding.fraJsonNode(data) }.getOrElse { cause ->
                val logId = UUID.randomUUID()
                logg.error(cause) { "Kunne ikke tolke søknad data, har datamodellen endret seg? Se igjennom endringene og revurder hva vi deler med kommunene før datamodellen oppdateres. (ref.: $logId)" }
                synchronized(hentSoknaderForKommuneApietSistRapportertSlack) {
                    if (
                        hentSoknaderForKommuneApietSistRapportertSlack.isBefore(
                            LocalDateTime.now().minusHours(1),
                        ) &&
                        hentSoknaderForKommuneApietSistRapportertSlack.hour >= 8 &&
                        hentSoknaderForKommuneApietSistRapportertSlack.hour < 16 &&
                        hentSoknaderForKommuneApietSistRapportertSlack.dayOfWeek < DayOfWeek.SATURDAY &&
                        !Environment.current.tier.isLocal
                    ) {
                        hentSoknaderForKommuneApietSistRapportertSlack = LocalDateTime.now()
                        runBlocking(Dispatchers.IO) {
                            slack.sendMessage(
                                "hm-soknadsbehandling-db",
                                slackIconEmoji(":this-is-fine-fire:"),
                                if (Environment.current.tier.isProd) "#digihot-alerts" else "#digihot-alerts-dev",
                                "Søknad datamodellen har endret seg og kvittering av innsendte " +
                                    "søknader tilbake til kommunen er satt på pause inntil noen har " +
                                    "vurdert om endringene kan medføre juridiske utfordringer. Oppdater " +
                                    "no.nav.hjelpemidler.soknad.db.domain.kommuneapi.* og sørg for at " +
                                    "vi filtrerer ut verdier som ikke skal kvitteres tilbake. " +
                                    "Se <https://github.com/navikt/hm-soknadsbehandling-db/blob" +
                                    "/main/src/main/kotlin/no/nav/hjelpemidler/soknad/db/domain" +
                                    "/kommuneapi/Valideringsmodell.kt|Valideringsmodell.kt>.\n\n" +
                                    "Bør fikses ASAP.\n\nFeilmelding: søk etter uuid i kibana: $logId",
                            )
                        }
                    }
                }
                return@list null
            }

            // Ekstra sikkerhetssjekker
            if (validatedData.soknad.innsender?.organisasjoner?.any { it.kommunenummer == kommunenummer } != true) {
                // En av verdiene er null eller ingen av organisasjonene har kommunenummeret vi leter etter...
                error("Noe har gått galt med sikkerhetsmekanismene i SQL query: uventet formidler kommunenummer")
            }

            if (validatedData.soknad.bruker.kommunenummer != kommunenummer) {
                error("Noe har gått galt med sikkerhetsmekanismene i SQL query: uventet brukers kommunenummer")
            }

            // Filtrer ut ikke-relevante felter
            val filteredData = validatedData.filtrerForKommuneApiet()

            SøknadForKommuneApi(
                fnrBruker = it.string("FNR_BRUKER"),
                navnBruker = it.string("NAVN_BRUKER"),
                fnrInnsender = it.stringOrNull("FNR_INNSENDER"),
                soknadId = it.uuid("SOKNADS_ID"),
                soknad = filteredData,
                soknadGjelder = it.stringOrNull("SOKNAD_GJELDER"),
                opprettet = it.localDateTime("CREATED"),
            )
        }
    }
}
