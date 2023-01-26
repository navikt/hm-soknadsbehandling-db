package no.nav.hjelpemidler.soknad.db.db

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.hjelpemidler.http.slack.slack
import no.nav.hjelpemidler.http.slack.slackIconEmoji
import no.nav.hjelpemidler.soknad.db.Configuration
import no.nav.hjelpemidler.soknad.db.Profile
import no.nav.hjelpemidler.soknad.db.domain.BehovsmeldingType
import no.nav.hjelpemidler.soknad.db.domain.ForslagsmotorTilbehoer_HjelpemiddelListe
import no.nav.hjelpemidler.soknad.db.domain.ForslagsmotorTilbehoer_Hjelpemidler
import no.nav.hjelpemidler.soknad.db.domain.ForslagsmotorTilbehoer_Soknad
import no.nav.hjelpemidler.soknad.db.domain.PapirSøknadData
import no.nav.hjelpemidler.soknad.db.domain.SoknadData
import no.nav.hjelpemidler.soknad.db.domain.SoknadMedStatus
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.domain.StatusCountRow
import no.nav.hjelpemidler.soknad.db.domain.StatusMedÅrsak
import no.nav.hjelpemidler.soknad.db.domain.StatusRow
import no.nav.hjelpemidler.soknad.db.domain.SøknadForBruker
import no.nav.hjelpemidler.soknad.db.domain.UtgåttSøknad
import no.nav.hjelpemidler.soknad.db.domain.kommune_api.Behovsmelding
import no.nav.hjelpemidler.soknad.db.domain.kommune_api.SøknadForKommuneApi
import no.nav.hjelpemidler.soknad.db.metrics.Prometheus
import org.intellij.lang.annotations.Language
import org.postgresql.util.PGobject
import java.math.BigInteger
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import javax.sql.DataSource

private val logg = KotlinLogging.logger {}

internal interface SøknadStore {
    fun save(soknadData: SoknadData): Int
    fun savePapir(soknadData: PapirSøknadData): Int
    fun hentSoknad(soknadsId: UUID): SøknadForBruker?
    fun hentSoknaderForBruker(fnrBruker: String): List<SoknadMedStatus>
    fun hentSoknadData(soknadsId: UUID): SoknadData?
    fun oppdaterStatus(soknadsId: UUID, status: Status): Int
    fun oppdaterStatusMedÅrsak(statusMedÅrsak: StatusMedÅrsak): Int
    fun slettSøknad(soknadsId: UUID): Int
    fun slettUtløptSøknad(soknadsId: UUID): Int
    fun oppdaterJournalpostId(soknadsId: UUID, journalpostId: String): Int
    fun oppdaterOppgaveId(soknadsId: UUID, oppgaveId: String): Int
    fun hentFnrForSoknad(soknadsId: UUID): String
    fun hentSoknaderTilGodkjenningEldreEnn(dager: Int): List<UtgåttSøknad>
    fun soknadFinnes(soknadsId: UUID): Boolean
    fun hentSoknadOpprettetDato(soknadsId: UUID): Date?
    fun fnrOgJournalpostIdFinnes(fnrBruker: String, journalpostId: Int): Boolean
    fun initieltDatasettForForslagsmotorTilbehoer(): List<ForslagsmotorTilbehoer_Hjelpemidler>
    fun hentGodkjenteSoknaderUtenOppgaveEldreEnn(dager: Int): List<String>
    fun behovsmeldingTypeFor(soknadsId: UUID): BehovsmeldingType?
    fun tellStatuser(): List<StatusCountRow>
    fun hentStatuser(soknadsId: UUID): List<StatusRow>
    fun hentSoknaderForKommuneApiet(kommunenummer: String, nyereEnn: UUID?, nyereEnnTidsstempel: Long?): List<SøknadForKommuneApi>
}

internal class SøknadStorePostgres(private val ds: DataSource) : SøknadStore {

    override fun soknadFinnes(soknadsId: UUID): Boolean {
        @Language("PostgreSQL") val statement =
            """
                SELECT SOKNADS_ID
                FROM V1_SOKNAD
                WHERE SOKNADS_ID = ?
            """

        val uuid = time("soknad_eksisterer") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        statement,
                        soknadsId,
                    ).map {
                        UUID.fromString(it.string("SOKNADS_ID"))
                    }.asSingle
                )
            }
        }
        return uuid != null
    }

    override fun fnrOgJournalpostIdFinnes(fnrBruker: String, journalpostId: Int): Boolean {
        @Language("PostgreSQL") val statement =
            """
                SELECT SOKNADS_ID
                FROM V1_SOKNAD
                WHERE FNR_BRUKER = ? AND JOURNALPOSTID = ?
            """

        val uuid = time("soknad_eksisterer") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        statement,
                        fnrBruker,
                        journalpostId,
                    ).map {
                        it.uuid("SOKNADS_ID")
                    }.asSingle
                )
            }
        }
        return uuid != null
    }

    override fun hentSoknad(soknadsId: UUID): SøknadForBruker? {
        @Language("PostgreSQL") val statement =
            """
                SELECT soknad.SOKNADS_ID, soknad.DATA ->> 'behovsmeldingType' AS behovsmeldingType, soknad.JOURNALPOSTID, soknad.DATA, soknad.CREATED, soknad.KOMMUNENAVN, soknad.FNR_BRUKER, soknad.UPDATED, soknad.ER_DIGITAL, soknad.SOKNAD_GJELDER, status.STATUS, status.ARSAKER, 
                (CASE WHEN EXISTS (
                    SELECT 1 FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID AND STATUS IN  ('GODKJENT_MED_FULLMAKT')
                ) THEN true ELSE false END) as fullmakt
                FROM V1_SOKNAD AS soknad 
                LEFT JOIN V1_STATUS AS status
                ON status.ID = (
                    SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID ORDER BY created DESC LIMIT 1
                )
                WHERE soknad.SOKNADS_ID = ? AND status.STATUS <> ?
            """

        return time("hent_soknad") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        statement,
                        soknadsId,
                        "GODKJENT_MED_FULLMAKT",
                    ).map {
                        val status = Status.valueOf(it.string("STATUS"))
                        if (status.isSlettetEllerUtløpt() || !it.boolean("ER_DIGITAL")) {
                            SøknadForBruker.newEmptySøknad(
                                søknadId = it.uuid("SOKNADS_ID"),
                                behovsmeldingType = BehovsmeldingType.valueOf(
                                    it.stringOrNull("behovsmeldingType").let { it ?: "SØKNAD" }
                                ),
                                journalpostId = it.stringOrNull("JOURNALPOSTID"),
                                status = Status.valueOf(it.string("STATUS")),
                                fullmakt = it.boolean("fullmakt"),
                                datoOpprettet = it.sqlTimestamp("created"),
                                datoOppdatert = when {
                                    it.sqlTimestampOrNull("updated") != null -> it.sqlTimestamp("updated")
                                    else -> it.sqlTimestamp("created")
                                },
                                fnrBruker = it.string("FNR_BRUKER"),
                                er_digital = it.boolean("ER_DIGITAL"),
                                soknadGjelder = it.stringOrNull("SOKNAD_GJELDER"),
                                ordrelinjer = emptyList(),
                                fagsakId = null,
                                valgteÅrsaker = objectMapper.readValue(
                                    it.stringOrNull("ARSAKER") ?: "[]"
                                ),
                            )
                        } else {
                            SøknadForBruker.new(
                                søknadId = it.uuid("SOKNADS_ID"),
                                behovsmeldingType = BehovsmeldingType.valueOf(
                                    it.stringOrNull("behovsmeldingType").let { it ?: "SØKNAD" }
                                ),
                                journalpostId = it.stringOrNull("JOURNALPOSTID"),
                                status = Status.valueOf(it.string("STATUS")),
                                fullmakt = it.boolean("fullmakt"),
                                datoOpprettet = it.sqlTimestamp("created"),
                                datoOppdatert = when {
                                    it.sqlTimestampOrNull("updated") != null -> it.sqlTimestamp("updated")
                                    else -> it.sqlTimestamp("created")
                                },
                                søknad = it.jsonNodeOrDefault("DATA", "{}"),
                                kommunenavn = it.stringOrNull("KOMMUNENAVN"),
                                fnrBruker = it.string("FNR_BRUKER"),
                                er_digital = it.boolean("ER_DIGITAL"),
                                soknadGjelder = it.stringOrNull("SOKNAD_GJELDER"),
                                ordrelinjer = emptyList(),
                                fagsakId = null,
                                valgteÅrsaker = objectMapper.readValue(
                                    it.stringOrNull("ARSAKER") ?: "[]"
                                ),
                            )
                        }
                    }.asSingle
                )
            }
        }
    }

    override fun hentSoknadOpprettetDato(soknadsId: UUID): Date? {
        @Language("PostgreSQL") val statement =
            """
                SELECT CREATED
                FROM V1_SOKNAD
                WHERE SOKNADS_ID = ?
            """

        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    statement,
                    soknadsId,
                ).map {
                    it.sqlTimestamp("created")
                }.asSingle
            )
        }
    }

    override fun hentSoknadData(soknadsId: UUID): SoknadData? {
        @Language("PostgreSQL") val statement =
            """
                SELECT soknad.SOKNADS_ID, soknad.FNR_BRUKER, soknad.NAVN_BRUKER, soknad.FNR_INNSENDER, soknad.DATA, soknad.KOMMUNENAVN, soknad.ER_DIGITAL, soknad.SOKNAD_GJELDER, status.STATUS
                FROM V1_SOKNAD AS soknad
                LEFT JOIN V1_STATUS AS status
                ON status.ID = (
                    SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID ORDER BY created DESC LIMIT 1
                )
                WHERE soknad.SOKNADS_ID = ?
            """

        return time("hent_soknaddata") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        statement,
                        soknadsId,
                    ).map {
                        SoknadData(
                            fnrBruker = it.string("FNR_BRUKER"),
                            navnBruker = it.string("NAVN_BRUKER"),
                            fnrInnsender = it.stringOrNull("FNR_INNSENDER"),
                            soknadId = it.uuid("SOKNADS_ID"),
                            status = Status.valueOf(it.string("STATUS")),
                            soknad = it.jsonNodeOrDefault("DATA", "{}"),
                            kommunenavn = it.stringOrNull("KOMMUNENAVN"),
                            er_digital = it.boolean("ER_DIGITAL"),
                            soknadGjelder = it.stringOrNull("SOKNAD_GJELDER"),
                        )
                    }.asSingle
                )
            }
        }
    }

    override fun hentFnrForSoknad(soknadsId: UUID): String {
        @Language("PostgreSQL") val statement =
            """
                SELECT FNR_BRUKER
                FROM V1_SOKNAD
                WHERE SOKNADS_ID = ?
            """

        val fnrBruker =
            time("hent_soknad") {
                using(sessionOf(ds)) { session ->
                    session.run(
                        queryOf(
                            statement,
                            soknadsId,
                        ).map {
                            it.string("FNR_BRUKER")
                        }.asSingle
                    )
                }
            }

        if (fnrBruker == null) {
            throw RuntimeException("No søknad with FNR found for soknadsId $soknadsId")
        } else {
            return fnrBruker
        }
    }

    override fun oppdaterStatusMedÅrsak(statusMedÅrsak: StatusMedÅrsak): Int =
        time("oppdater_status med årsak") {
            using(sessionOf(ds)) { session ->
                if (checkIfLastStatusMatches(session, statusMedÅrsak.søknadId, statusMedÅrsak.status)) return@using 0
                val result = session.transaction { transaction ->
                    // Add the new status to the status table
                    transaction.run(
                        queryOf(
                            "INSERT INTO V1_STATUS (SOKNADS_ID, STATUS, BEGRUNNELSE, ARSAKER) VALUES (?, ?, ?, ?)",
                            statusMedÅrsak.søknadId,
                            statusMedÅrsak.status.name,
                            statusMedÅrsak.begrunnelse,
                            statusMedÅrsak.valgteÅrsaker?.let {
                                PGobject().apply {
                                    type = "jsonb"
                                    value = valgteÅrsakerToJsonString(it)
                                }
                            }
                        ).asUpdate
                    )
                    // Oppdatere UPDATED felt når man legger til ny status for søknad
                    transaction.run(
                        queryOf(
                            "UPDATE V1_SOKNAD SET UPDATED = now() WHERE SOKNADS_ID = ?",
                            statusMedÅrsak.søknadId,
                        ).asUpdate
                    )
                }

                return@using result
            }
        }

    override fun oppdaterStatus(soknadsId: UUID, status: Status): Int =
        time("oppdater_status") {
            using(sessionOf(ds)) { session ->
                if (checkIfLastStatusMatches(session, soknadsId, status)) return@using 0
                val result = session.transaction { transaction ->
                    // Add the new status to the status table
                    transaction.run(
                        queryOf(
                            "INSERT INTO V1_STATUS (SOKNADS_ID, STATUS) VALUES (?, ?)",
                            soknadsId,
                            status.name
                        ).asUpdate
                    )
                    // Oppdatere UPDATED felt når man legger til ny status for søknad
                    transaction.run(
                        queryOf(
                            "UPDATE V1_SOKNAD SET UPDATED = now() WHERE SOKNADS_ID = ?",
                            soknadsId,
                        ).asUpdate
                    )
                }

                return@using result
            }
        }

    override fun slettSøknad(soknadsId: UUID) = slettSøknad(soknadsId, Status.SLETTET)

    override fun slettUtløptSøknad(soknadsId: UUID) = slettSøknad(soknadsId, Status.UTLØPT)

    private fun slettSøknad(soknadsId: UUID, status: Status): Int =
        time("slett_soknad") {
            using(sessionOf(ds)) { session ->
                if (checkIfLastStatusMatches(session, soknadsId, status)) return@using 0
                session.transaction { transaction ->
                    transaction.run(
                        queryOf(
                            "INSERT INTO V1_STATUS (SOKNADS_ID, STATUS) VALUES (?, ?)",
                            soknadsId,
                            status.name
                        ).asUpdate
                    )
                    transaction.run(
                        queryOf(
                            "UPDATE V1_SOKNAD SET UPDATED = now(), DATA = NULL WHERE SOKNADS_ID = ?",
                            soknadsId,
                        ).asUpdate
                    )
                }
            }
        }

    override fun oppdaterJournalpostId(soknadsId: UUID, journalpostId: String): Int {
        val bigIntJournalPostId = BigInteger(journalpostId)
        return time("oppdater_journalpostId") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "UPDATE V1_SOKNAD SET JOURNALPOSTID = ?, UPDATED = now() WHERE SOKNADS_ID = ?",
                        bigIntJournalPostId,
                        soknadsId
                    ).asUpdate
                )
            }
        }
    }

    override fun oppdaterOppgaveId(soknadsId: UUID, oppgaveId: String): Int {
        val bigIntOppgaveId = BigInteger(oppgaveId)
        return time("oppdater_oppgaveId") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "UPDATE V1_SOKNAD SET OPPGAVEID = ?, UPDATED = now() WHERE SOKNADS_ID = ? AND OPPGAVEID IS NULL",
                        bigIntOppgaveId,
                        soknadsId
                    ).asUpdate
                )
            }
        }
    }

    override fun hentSoknaderForBruker(fnrBruker: String): List<SoknadMedStatus> {
        @Language("PostgreSQL") val statement =
            """
                SELECT soknad.SOKNADS_ID, soknad.DATA ->> 'behovsmeldingType' AS behovsmeldingType, soknad.JOURNALPOSTID, soknad.CREATED, soknad.UPDATED, soknad.DATA, soknad.ER_DIGITAL, soknad.SOKNAD_GJELDER, status.STATUS, status.ARSAKER,
                (CASE WHEN EXISTS (
                    SELECT 1 FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID AND STATUS IN  ('GODKJENT_MED_FULLMAKT')
                ) THEN true ELSE false END) as fullmakt
                FROM V1_SOKNAD AS soknad
                LEFT JOIN V1_STATUS AS status
                ON status.ID = (
                    SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID ORDER BY created DESC LIMIT 1
                )
                WHERE soknad.FNR_BRUKER = ? AND status.STATUS <> ?
                ORDER BY soknad.CREATED DESC
            """

        return time("hent_soknader_for_bruker") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        statement,
                        fnrBruker,
                        "GODKJENT_MED_FULLMAKT",
                    ).map {
                        val status = Status.valueOf(it.string("STATUS"))
                        if (status.isSlettetEllerUtløpt() || !it.boolean("ER_DIGITAL")) {
                            SoknadMedStatus.newSøknadUtenFormidlernavn(
                                soknadId = it.uuid("SOKNADS_ID"),
                                behovsmeldingType = BehovsmeldingType.valueOf(
                                    it.stringOrNull("behovsmeldingType").let { it ?: "SØKNAD" }
                                ),
                                journalpostId = it.stringOrNull("JOURNALPOSTID"),
                                status = Status.valueOf(it.string("STATUS")),
                                fullmakt = it.boolean("fullmakt"),
                                datoOpprettet = it.sqlTimestamp("created"),
                                datoOppdatert = when {
                                    it.sqlTimestampOrNull("updated") != null -> it.sqlTimestamp("updated")
                                    else -> it.sqlTimestamp("created")
                                },
                                er_digital = it.boolean("ER_DIGITAL"),
                                soknadGjelder = it.stringOrNull("SOKNAD_GJELDER"),
                                valgteÅrsaker = objectMapper.readValue(
                                    it.stringOrNull("ARSAKER") ?: "[]"
                                ),
                            )
                        } else {
                            SoknadMedStatus.newSøknadMedFormidlernavn(
                                soknadId = it.uuid("SOKNADS_ID"),
                                behovsmeldingType = BehovsmeldingType.valueOf(
                                    it.stringOrNull("behovsmeldingType").let { it ?: "SØKNAD" }
                                ),
                                journalpostId = it.stringOrNull("JOURNALPOSTID"),
                                status = Status.valueOf(it.string("STATUS")),
                                fullmakt = it.boolean("fullmakt"),
                                datoOpprettet = it.sqlTimestamp("created"),
                                datoOppdatert = when {
                                    it.sqlTimestampOrNull("updated") != null -> it.sqlTimestamp("updated")
                                    else -> it.sqlTimestamp("created")
                                },
                                søknad = it.jsonNodeOrDefault("DATA", "{}"),
                                er_digital = it.boolean("ER_DIGITAL"),
                                soknadGjelder = it.stringOrNull("SOKNAD_GJELDER"),
                                valgteÅrsaker = objectMapper.readValue(
                                    it.stringOrNull("ARSAKER") ?: "[]"
                                ),
                            )
                        }
                    }.asList
                )
            }
        }
    }

    override fun hentSoknaderTilGodkjenningEldreEnn(dager: Int): List<UtgåttSøknad> {
        @Language("PostgreSQL") val statement =
            """
                SELECT soknad.SOKNADS_ID, soknad.FNR_BRUKER, status.STATUS
                FROM V1_SOKNAD AS soknad
                LEFT JOIN V1_STATUS AS status
                ON status.ID = (
                    SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID ORDER BY created DESC LIMIT 1
                )
                WHERE status.STATUS = ? AND (soknad.CREATED + interval '$dager day') < now()
                ORDER BY soknad.CREATED DESC
            """

        return time("utgåtte_søknader") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        statement,
                        Status.VENTER_GODKJENNING.name,
                    ).map {
                        UtgåttSøknad(
                            søknadId = it.uuid("SOKNADS_ID"),
                            status = Status.valueOf(it.string("STATUS")),
                            fnrBruker = it.string("FNR_BRUKER")
                        )
                    }.asList
                )
            }
        }
    }

    override fun save(soknadData: SoknadData): Int =
        time("insert_soknad") {
            using(sessionOf(ds)) { session ->
                session.transaction { transaction ->
                    // Add the new status to the status table
                    if (!checkIfLastStatusMatches(transaction, soknadData.soknadId, soknadData.status)) transaction.run(
                        queryOf(
                            "INSERT INTO V1_STATUS (SOKNADS_ID, STATUS) VALUES (?, ?)",
                            soknadData.soknadId,
                            soknadData.status.name,
                        ).asUpdate
                    )
                    // Add the new Søknad into the Søknad table
                    transaction.run(
                        queryOf(
                            "INSERT INTO V1_SOKNAD (SOKNADS_ID, FNR_BRUKER, NAVN_BRUKER, FNR_INNSENDER, DATA, KOMMUNENAVN, ER_DIGITAL, SOKNAD_GJELDER) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                            soknadData.soknadId,
                            soknadData.fnrBruker,
                            soknadData.navnBruker,
                            soknadData.fnrInnsender,
                            PGobject().apply {
                                type = "jsonb"
                                value = soknadToJsonString(soknadData.soknad)
                            },
                            soknadData.kommunenavn,
                            true,
                            soknadData.soknadGjelder ?: "Søknad om hjelpemidler",
                        ).asUpdate
                    )
                }
            }
        }

    private fun checkIfLastStatusMatches(session: Session, soknadsId: UUID, status: Status): Boolean {
        val result = session.run(
            queryOf(
                "SELECT STATUS FROM V1_STATUS WHERE ID = (SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = ? ORDER BY created DESC LIMIT 1)",
                soknadsId
            ).map {
                it.stringOrNull("STATUS")
            }.asSingle
        ) ?: return false /* special case where there is no status in the database (søknad is being added now) */
        if (result != status.name) return false
        return true
    }

    override fun savePapir(soknadData: PapirSøknadData): Int =
        time("insert_papirsoknad") {
            using(sessionOf(ds)) { session ->
                session.transaction { transaction ->
                    if (!checkIfLastStatusMatches(transaction, soknadData.soknadId, soknadData.status)) transaction.run(
                        queryOf(
                            "INSERT INTO V1_STATUS (SOKNADS_ID, STATUS) VALUES (?, ?)",
                            soknadData.soknadId,
                            soknadData.status.name,
                        ).asUpdate
                    )
                    transaction.run(
                        queryOf(
                            "INSERT INTO V1_SOKNAD (SOKNADS_ID,FNR_BRUKER, ER_DIGITAL, JOURNALPOSTID, NAVN_BRUKER ) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
                            soknadData.soknadId,
                            soknadData.fnrBruker,
                            false,
                            soknadData.journalpostid,
                            soknadData.navnBruker
                        ).asUpdate
                    )
                }
            }
        }

    override fun initieltDatasettForForslagsmotorTilbehoer(): List<ForslagsmotorTilbehoer_Hjelpemidler> {
        @Language("PostgreSQL") val statement =
            """
                SELECT DATA, CREATED FROM V1_SOKNAD WHERE ER_DIGITAL AND DATA IS NOT NULL
            """

        val soknader = time("initieltDatasettForForslagsmotorTilbehoer") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        statement,
                    ).map {
                        val hjelpemiddel =
                            objectMapper.readValue<ForslagsmotorTilbehoer_Hjelpemidler>(it.string("DATA"))
                        hjelpemiddel.created = it.localDateTime("CREATED")
                        hjelpemiddel
                    }.asList
                )
            }
        }

        // Filter out products with no accessories (ca. 2/3 of the cases)
        return soknader.map { soknad ->
            ForslagsmotorTilbehoer_Hjelpemidler(
                soknad = ForslagsmotorTilbehoer_Soknad(
                    id = soknad.soknad.id,
                    hjelpemidler = ForslagsmotorTilbehoer_HjelpemiddelListe(
                        hjelpemiddelListe = soknad.soknad.hjelpemidler.hjelpemiddelListe.filter {
                            it.tilbehorListe?.isNotEmpty() ?: false
                        }.toTypedArray(),
                    ),
                ),
                created = soknad.created,
            )
        }
    }

    override fun hentGodkjenteSoknaderUtenOppgaveEldreEnn(dager: Int): List<String> {
        @Language("PostgreSQL") val statement =
            """
                SELECT soknad.soknads_id
                FROM V1_SOKNAD AS soknad
                LEFT JOIN V1_STATUS AS status
                ON status.ID = (
                    SELECT ID FROM V1_STATUS WHERE SOKNADS_ID = soknad.SOKNADS_ID ORDER BY created DESC LIMIT 1
                )
                WHERE status.STATUS IN (?, ?) 
                    AND (soknad.CREATED + interval '$dager day') < now() 
                    AND soknad.oppgaveid IS NULL
                    AND soknad.created > '2021-04-13' -- OPPGAVEID kolonnen ble lagt til 2021-04-12. Alt før dette har OPPGAVEID == NULL
            """

        return time("godkjente_soknader_uten_oppgave") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        statement,
                        Status.GODKJENT_MED_FULLMAKT.name,
                        Status.GODKJENT.name,
                    ).map {
                        it.string("SOKNADS_ID")
                    }.asList
                )
            }
        }
    }

    override fun behovsmeldingTypeFor(soknadsId: UUID): BehovsmeldingType? {
        @Language("PostgreSQL") val statement =
            """
                SELECT soknad.DATA ->> 'behovsmeldingType' AS behovsmeldingType
                FROM V1_SOKNAD AS soknad
                WHERE soknad.SOKNADS_ID = ?
            """

        return time("behovsmeldingTypeFor") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        statement,
                        soknadsId,
                    ).map {
                        BehovsmeldingType.valueOf(it.stringOrNull("behovsmeldingType").let { it ?: "SØKNAD" })
                    }.asSingle
                )
            }
        }
    }

    override fun tellStatuser(): List<StatusCountRow> {
        @Language("PostgreSQL") val statement =
            """
                SELECT STATUS AS STATUS, COUNT(SOKNADS_ID) AS COUNT FROM (
                SELECT V1_STATUS.SOKNADS_ID, V1_STATUS.STATUS FROM V1_STATUS
                                    LEFT JOIN V1_STATUS last_status ON
                            V1_STATUS.SOKNADS_ID = last_status.SOKNADS_ID AND
                            V1_STATUS.created < last_status.created
                WHERE last_status.SOKNADS_ID IS NULL) last_statuses
                GROUP BY STATUS                                
            """.trimIndent()

        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    statement
                ).map {
                    StatusCountRow(
                        Status.valueOf(it.string("STATUS")),
                        it.int("COUNT"),
                    )
                }.asList
            )
        }
    }

    override fun hentStatuser(soknadsId: UUID): List<StatusRow> {
        @Language("PostgreSQL") val statement = """
            SELECT STATUS, V1_STATUS.CREATED AS CREATED, ER_DIGITAL 
            FROM V1_STATUS JOIN V1_SOKNAD ON V1_STATUS.SOKNADS_ID = V1_SOKNAD.SOKNADS_ID 
            WHERE V1_STATUS.SOKNADS_ID = ? ORDER BY CREATED DESC
        """.trimIndent() // ORDER is just a preventative measure

        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    statement,
                    soknadsId
                ).map {
                    StatusRow(
                        Status.valueOf(it.string("STATUS")),
                        it.sqlTimestamp("CREATED"),
                        it.boolean("ER_DIGITAL"),
                    )
                }.asList
            )
        }
    }

    private var hentSoknaderForKommuneApietSistRapportertSlack = LocalDateTime.now().minusHours(2)
    override fun hentSoknaderForKommuneApiet(kommunenummer: String, nyereEnn: UUID?, nyereEnnTidsstempel: Long?): List<SøknadForKommuneApi> {
        val extraWhere1 = if (nyereEnn == null) "" else "AND CREATED > (SELECT CREATED FROM V1_SOKNAD WHERE SOKNADS_ID = :nyereEnn)"
        val extraWhere2 = if (nyereEnnTidsstempel == null) "" else "AND CREATED > :nyereEnnTidsstempel"

        @Language("PostgreSQL") val statement =
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
                ORDER BY CREATED DESC
                ;
            """

        return time("hentSoknaderForKommuneApiet") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        statement,
                        mapOf(
                            "kommunenummer" to kommunenummer,
                            "kommunenummerJson" to """[{"kommunenummer": "$kommunenummer"}]""",
                            "nyereEnn" to nyereEnn,
                            "nyereEnnTidsstempel" to nyereEnnTidsstempel?.let { nyereEnnTidsstempel ->
                                LocalDateTime.ofInstant(Instant.ofEpochSecond(nyereEnnTidsstempel), ZoneId.systemDefault())
                            },
                        )
                    ).map {
                        val data = it.jsonNodeOrDefault("DATA", "{}")

                        // Valider data-feltet, og hvis ikke filtrer ut raden ved å returnere null-verdi
                        val validatedData = kotlin.runCatching { Behovsmelding.fraJsonNode(data) }.getOrElse { cause ->
                            logg.error(cause) { "Kunne ikke tolke søknad data, har datamodellen endret seg? Se igjennom endringene og revurder hva vi deler med kommunene før datamodellen oppdateres." }
                            synchronized(hentSoknaderForKommuneApietSistRapportertSlack) {
                                if (
                                    hentSoknaderForKommuneApietSistRapportertSlack.isBefore(LocalDateTime.now().minusHours(1)) &&
                                    hentSoknaderForKommuneApietSistRapportertSlack.hour >= 8 &&
                                    hentSoknaderForKommuneApietSistRapportertSlack.hour < 16 &&
                                    hentSoknaderForKommuneApietSistRapportertSlack.dayOfWeek < DayOfWeek.SATURDAY &&
                                    Configuration.application.profile != Profile.LOCAL
                                ) {
                                    hentSoknaderForKommuneApietSistRapportertSlack = LocalDateTime.now()
                                    runBlocking {
                                        slack().sendMessage(
                                            "hm-soknadsbehandling-db",
                                            slackIconEmoji(":this-is-fine-fire:"),
                                            if (Configuration.application.profile == Profile.PROD) "#digihot-alerts" else "#digihot-alerts-dev",
                                            "Søknad datamodellen har endret seg og kvittering av innsendte " +
                                                "søknader tilbake til kommunen er satt på pause inntil noen har " +
                                                "vurdert om endringene kan medføre juridiske utfordringer. Oppdater " +
                                                "no.nav.hjelpemidler.soknad.db.domain.kommune_api.* og sørg for at " +
                                                "vi filtrerer ut verdier som ikke skal kvitteres tilbake. " +
                                                "Se <https://github.com/navikt/hm-soknadsbehandling-db/blob" +
                                                "/main/src/main/kotlin/no/nav/hjelpemidler/soknad/db/domain" +
                                                "/kommune_api/Valideringsmodell.kt|Valideringsmodell.kt>.\n\n" +
                                                "Bør fikses ASAP.\n\nFeilmelding[..:2000]:\n```" +
                                                (cause.message?.take(2000) ?: "<Ingen melding>") +
                                                "```"
                                        )
                                    }
                                }
                            }
                            return@map null
                        }

                        // Ekstra sikkerhetssjekker
                        if (validatedData.soknad.innsender?.organisasjoner?.any { it.kommunenummer == kommunenummer } != true) {
                            // En av verdiene er null eller ingen av organisasjonene har kommunenummeret vi leter etter...
                            throw RuntimeException("Noe har gått galt med sikkerhetsmekanismene i SQL query: uventet formidler kommunenummer")
                        }

                        if (validatedData.soknad.bruker.kommunenummer != kommunenummer) {
                            throw RuntimeException("Noe har gått galt med sikkerhetsmekanismene i SQL query: uventet brukers kommunenummer")
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
                    }.asList
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

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    private fun soknadToJsonString(soknad: JsonNode): String = objectMapper.writeValueAsString(soknad)

    private fun valgteÅrsakerToJsonString(valgteÅrsaker: Set<String>): String =
        objectMapper.writeValueAsString(valgteÅrsaker)
}
