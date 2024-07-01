package no.nav.hjelpemidler.soknad.db.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.hjelpemidler.database.pgJsonbOf
import no.nav.hjelpemidler.soknad.db.client.hmdb.HjelpemiddeldatabaseClient
import no.nav.hjelpemidler.soknad.db.domain.HarOrdre
import no.nav.hjelpemidler.soknad.db.domain.OrdrelinjeData
import no.nav.hjelpemidler.soknad.db.domain.SøknadForBrukerOrdrelinje
import java.util.UUID
import javax.sql.DataSource

interface OrdreStore {
    fun save(ordrelinje: OrdrelinjeData): Int
    fun ordreSisteDøgn(soknadsId: UUID): HarOrdre
    fun harOrdre(soknadsId: UUID): HarOrdre
    suspend fun ordreForSoknad(soknadsId: UUID): List<SøknadForBrukerOrdrelinje>
}

class OrdreStorePostgres(private val ds: DataSource) : OrdreStore {
    override fun save(ordrelinje: OrdrelinjeData): Int {
        return time("insert_ordrelinje") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "INSERT INTO V1_OEBS_DATA (SOKNADS_ID, OEBS_ID, FNR_BRUKER, SERVICEFORESPOERSEL, ORDRENR, ORDRELINJE, DELORDRELINJE, ARTIKKELNR, ANTALL, ENHET, PRODUKTGRUPPE, PRODUKTGRUPPENR, HJELPEMIDDELTYPE, DATA) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",
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
                        ordrelinje.hjelpemiddeltype,
                        pgJsonbOf(ordrelinje.data),
                    ).asUpdate,
                )
            }
        }
    }

    override fun ordreSisteDøgn(soknadsId: UUID): HarOrdre {
        val query =
            """
            SELECT HJELPEMIDDELTYPE
            FROM V1_OEBS_DATA 
            WHERE created > NOW() - '24 hours'::interval
            AND SOKNADS_ID = ?
            GROUP BY HJELPEMIDDELTYPE
            """.trimIndent()

        val result = time("order_siste_doegn") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        query,
                        soknadsId,
                    ).map {
                        it.string("HJELPEMIDDELTYPE")
                    }.asList,
                )
            }
        }

        return HarOrdre(
            harOrdreAvTypeHjelpemidler = result.any { it != "Del" },
            harOrdreAvTypeDel = result.any { it == "Del" },
        )
    }

    override fun harOrdre(soknadsId: UUID): HarOrdre {
        val query =
            """
            SELECT HJELPEMIDDELTYPE
            FROM V1_OEBS_DATA
            WHERE SOKNADS_ID = ?
            GROUP BY HJELPEMIDDELTYPE
            """.trimIndent()

        val result = time("har_ordre") {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        query,
                        soknadsId,
                    ).map {
                        it.string("HJELPEMIDDELTYPE")
                    }.asList,
                )
            }
        }

        return HarOrdre(
            harOrdreAvTypeHjelpemidler = result.any { it != "Del" },
            harOrdreAvTypeDel = result.any { it == "Del" },
        )
    }

    override suspend fun ordreForSoknad(soknadsId: UUID): List<SøknadForBrukerOrdrelinje> {
        val ordrelinjer = withContext(Dispatchers.IO) {
            using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        """
                        SELECT ARTIKKELNR, DATA ->> 'artikkelbeskrivelse' AS ARTIKKELBESKRIVELSE, ANTALL, PRODUKTGRUPPE, CREATED
                        FROM V1_OEBS_DATA
                        WHERE SOKNADS_ID = ? AND HJELPEMIDDELTYPE <> 'Del'
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
                    }.asList,
                )
            }
        }

        val produkter = HjelpemiddeldatabaseClient
            .hentProdukterMedHmsnrs(ordrelinjer.map { it.artikkelNr }.toSet())
            .groupBy { it.hmsArtNr }
            .mapValues { it.value.first() }

        return ordrelinjer.map {
            it.berik(produkter[it.artikkelNr])
        }
    }
}
