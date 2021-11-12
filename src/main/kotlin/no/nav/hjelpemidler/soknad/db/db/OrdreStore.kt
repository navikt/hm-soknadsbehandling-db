package no.nav.hjelpemidler.soknad.db.db

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.hjelpemidler.soknad.db.client.hmdb.HjelpemiddeldatabaseClient
import no.nav.hjelpemidler.soknad.db.domain.OrdrelinjeData
import no.nav.hjelpemidler.soknad.db.domain.SøknadForBrukerOrdrelinje
import no.nav.hjelpemidler.soknad.db.metrics.Prometheus
import org.postgresql.util.PGobject
import java.util.UUID
import javax.sql.DataSource
import kotlin.time.ExperimentalTime

internal interface OrdreStore {
    fun save(ordrelinje: OrdrelinjeData): Int
    fun ordreSisteDøgn(soknadsId: UUID): Boolean
    suspend fun ordreForSoknad(soknadsId: UUID): List<SøknadForBrukerOrdrelinje>
}

internal class OrdreStorePostgres(private val ds: DataSource) : OrdreStore {

    override fun save(ordrelinje: OrdrelinjeData): Int {
        return time("insert_ordrelinje") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "INSERT INTO V1_OEBS_DATA (SOKNADS_ID, OEBS_ID, FNR_BRUKER, SERVICEFORESPOERSEL, ORDRENR, ORDRELINJE, DELORDRELINJE, ARTIKKELNR, ANTALL, ENHET, PRODUKTGRUPPE, PRODUKTGRUPPENR, DATA) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                        ordrelinje.søknadId,
                        ordrelinje.oebsId,
                        ordrelinje.fnrBruker,
                        ordrelinje.serviceforespørsel,
                        ordrelinje.ordrenr,
                        ordrelinje.ordrelinje,
                        ordrelinje.delordrelinje,
                        ordrelinje.artikkelnr,
                        ordrelinje.antall,
                        ordrelinje.enhet,
                        ordrelinje.produktgruppe,
                        ordrelinje.produktgruppeNr,
                        PGobject().apply {
                            type = "jsonb"
                            value = ordrelinjeToJsonString(ordrelinje.data)
                        },
                    ).asUpdate
                )
            }
        }
    }

    override fun ordreSisteDøgn(soknadsId: UUID): Boolean {
        val query =
            """
            SELECT 1
            FROM V1_OEBS_DATA 
            WHERE  created > NOW() - '24 hours'::interval AND SOKNADS_ID = ?
            """.trimIndent()

        val result = time("order_siste_doegn") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        query,
                        soknadsId
                    ).map {
                        true
                    }.asSingle
                )
            }
        }

        return result != null
    }

    @ExperimentalTime
    override suspend fun ordreForSoknad(soknadsId: UUID): List<SøknadForBrukerOrdrelinje> {
        val ordrelinjer = withContext(Dispatchers.IO) {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        """
                        SELECT ARTIKKELNR, DATA ->> 'artikkelbeskrivelse' AS ARTIKKELBESKRIVELSE, ANTALL, PRODUKTGRUPPE, CREATED
                        FROM V1_OEBS_DATA
                        WHERE SOKNADS_ID = ?
                        """.trimIndent(),
                        soknadsId,
                    ).map {
                        SøknadForBrukerOrdrelinje(
                            antall = it.double("ANTALL"),
                            antallEnhet = "STK",
                            kategori = it.string("PRODUKTGRUPPE"),
                            artikkelBeskrivelse = it.string("ARTIKKELBESKRIVELSE"),
                            artikkelNr = it.string("ARTIKKELNR"),
                            datoUtsendelse = it.localDateOrNull("CREATED").toString(),
                        )
                    }.asList
                )
            }
        }

        val produkter = HjelpemiddeldatabaseClient
            .hentProdukterMedHmsnrs(ordrelinjer.map { it.artikkelNr }.toSet())
            .groupBy { it.hmsnr }
            .mapValues { it.value.first() }

        return ordrelinjer.map {
            it.berik(produkter[it.artikkelNr])
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

    private fun ordrelinjeToJsonString(ordrelinje: JsonNode): String = objectMapper.writeValueAsString(ordrelinje)
}
