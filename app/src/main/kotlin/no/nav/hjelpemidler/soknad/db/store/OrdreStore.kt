package no.nav.hjelpemidler.soknad.db.store

import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.pgJsonbOf
import no.nav.hjelpemidler.soknad.db.domain.HarOrdre
import no.nav.hjelpemidler.soknad.db.domain.OrdrelinjeData
import no.nav.hjelpemidler.soknad.db.domain.SøknadForBrukerOrdrelinje
import java.util.UUID

interface OrdreStore {
    fun save(ordrelinje: OrdrelinjeData): Int
    fun ordreSisteDøgn(søknadId: UUID): HarOrdre
    fun harOrdre(søknadId: UUID): HarOrdre
    fun ordreForSoknad(søknadId: UUID): List<SøknadForBrukerOrdrelinje>
}

class OrdreStorePostgres(private val tx: JdbcOperations) : OrdreStore {
    override fun save(ordrelinje: OrdrelinjeData): Int {
        return time("insert_ordrelinje") {
            tx.update(
                """
                    INSERT INTO v1_oebs_data (soknads_id, oebs_id, fnr_bruker, serviceforespoersel, ordrenr, ordrelinje, delordrelinje,
                                              artikkelnr, antall, enhet, produktgruppe, produktgruppenr, hjelpemiddeltype, data)
                    VALUES (:soknadId, :oebsId, :fnrBruker, :serviceforesporsel, :ordrenr, :ordrelinje, :delordrelinje,
                            :artikkelnr, :antall, :enhet, :produktgruppe, :produktgruppeNr, :hjelpemiddeltype, :data)
                    ON CONFLICT DO NOTHING
                """.trimIndent(),
                mapOf(
                    "soknadId" to ordrelinje.søknadId,
                    "oebsId" to ordrelinje.oebsId,
                    "fnrBruker" to ordrelinje.fnrBruker,
                    "serviceforesporsel" to ordrelinje.serviceforespørsel,
                    "ordrenr" to ordrelinje.ordrenr,
                    "ordrelinje" to ordrelinje.ordrelinje,
                    "delordrelinje" to ordrelinje.delordrelinje,
                    "artikkelnr" to ordrelinje.artikkelnr,
                    "antall" to ordrelinje.antall,
                    "enhet" to ordrelinje.enhet,
                    "produktgruppe" to ordrelinje.produktgruppe,
                    "produktgruppeNr" to ordrelinje.produktgruppeNr,
                    "hjelpemiddeltype" to ordrelinje.hjelpemiddeltype,
                    "data" to pgJsonbOf(ordrelinje.data),
                ),
            ).actualRowCount
        }
    }

    override fun ordreSisteDøgn(søknadId: UUID): HarOrdre {
        val result = time("order_siste_doegn") {
            tx.list(
                """
                    SELECT hjelpemiddeltype
                    FROM v1_oebs_data 
                    WHERE created > NOW() - '24 hours'::INTERVAL
                      AND soknads_id = :soknadId
                    GROUP BY hjelpemiddeltype
                """.trimIndent(),
                mapOf("soknadId" to søknadId),
            ) { it.string("hjelpemiddeltype") }
        }
        return HarOrdre(
            harOrdreAvTypeHjelpemidler = result.any { it != "Del" },
            harOrdreAvTypeDel = result.any { it == "Del" },
        )
    }

    override fun harOrdre(søknadId: UUID): HarOrdre {
        val result = time("order_siste_doegn") {
            tx.list(
                """
                    SELECT hjelpemiddeltype
                    FROM v1_oebs_data
                    WHERE soknads_id = :soknadId
                    GROUP BY hjelpemiddeltype
                """.trimIndent(),
                mapOf("soknadId" to søknadId),
            ) { it.string("hjelpemiddeltype") }
        }
        return HarOrdre(
            harOrdreAvTypeHjelpemidler = result.any { it != "Del" },
            harOrdreAvTypeDel = result.any { it == "Del" },
        )
    }

    override fun ordreForSoknad(søknadId: UUID): List<SøknadForBrukerOrdrelinje> {
        return tx.list(
            """
                SELECT artikkelnr,
                       data ->> 'artikkelbeskrivelse' AS artikkelbeskrivelse,
                       antall,
                       produktgruppe,
                       created
                FROM v1_oebs_data
                WHERE soknads_id = :soknadId AND hjelpemiddeltype <> 'Del'
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) {
            SøknadForBrukerOrdrelinje(
                antall = it.double("antall"),
                antallEnhet = "STK",
                kategori = it.string("produktgruppe"),
                artikkelBeskrivelse = it.string("artikkelbeskrivelse"),
                artikkelNr = it.string("artikkelnr"),
                datoUtsendelse = it.localDateOrNull("created").toString(),
            )
        }
    }
}
