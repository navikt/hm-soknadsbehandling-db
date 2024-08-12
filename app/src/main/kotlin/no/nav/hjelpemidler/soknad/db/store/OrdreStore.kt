package no.nav.hjelpemidler.soknad.db.store

import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadId
import no.nav.hjelpemidler.behovsmeldingsmodell.ordre.Ordrelinje
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.Store
import no.nav.hjelpemidler.database.json
import no.nav.hjelpemidler.database.pgJsonbOf
import no.nav.hjelpemidler.soknad.db.domain.HarOrdre
import no.nav.hjelpemidler.soknad.db.domain.SøknadForBrukerOrdrelinje

class OrdreStore(private val tx: JdbcOperations) : Store {
    fun lagre(søknadId: SøknadId, ordrelinje: Ordrelinje): Int {
        return tx.update(
            """
                INSERT INTO v1_oebs_data (soknads_id, oebs_id, fnr_bruker, serviceforespoersel, ordrenr, ordrelinje, delordrelinje,
                                          artikkelnr, antall, enhet, produktgruppe, produktgruppenr, hjelpemiddeltype, data)
                VALUES (:soknadId, :oebsId, :fnrBruker, :serviceforesporsel, :ordrenr, :ordrelinje, :delordrelinje,
                        :artikkelnr, :antall, :enhet, :produktgruppe, :produktgruppenr, :hjelpemiddeltype, :data)
                ON CONFLICT DO NOTHING
            """.trimIndent(),
            mapOf(
                "soknadId" to søknadId,
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
                "produktgruppenr" to ordrelinje.produktgruppenr,
                "hjelpemiddeltype" to ordrelinje.hjelpemiddeltype,
                "data" to pgJsonbOf(ordrelinje.data),
            ),
        ).actualRowCount
    }

    fun ordreSisteDøgn(søknadId: SøknadId): HarOrdre {
        val result = tx.list(
            """
                SELECT hjelpemiddeltype
                FROM v1_oebs_data
                WHERE created > NOW() - '24 hours'::INTERVAL
                  AND soknads_id = :soknadId
                GROUP BY hjelpemiddeltype
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) { it.string("hjelpemiddeltype") }
        return HarOrdre(
            harOrdreAvTypeHjelpemidler = result.any { it != "Del" },
            harOrdreAvTypeDel = result.any { it == "Del" },
        )
    }

    fun harOrdre(søknadId: SøknadId): HarOrdre {
        val result = tx.list(
            """
                SELECT hjelpemiddeltype
                FROM v1_oebs_data
                WHERE soknads_id = :soknadId
                GROUP BY hjelpemiddeltype
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) { it.string("hjelpemiddeltype") }
        return HarOrdre(
            harOrdreAvTypeHjelpemidler = result.any { it != "Del" },
            harOrdreAvTypeDel = result.any { it == "Del" },
        )
    }

    fun finnOrdreForSøknad(søknadId: SøknadId): List<SøknadForBrukerOrdrelinje> {
        return tx.list(
            """
                SELECT artikkelnr,
                       data ->> 'artikkelbeskrivelse' AS artikkelbeskrivelse,
                       antall,
                       produktgruppe,
                       created
                FROM v1_oebs_data
                WHERE soknads_id = :soknadId
                  AND hjelpemiddeltype <> 'Del'
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

    fun finnOrdreForSøknad2(søknadId: SøknadId): List<Ordrelinje> {
        return tx.list(
            """
                SELECT soknads_id,
                       fnr_bruker,
                       serviceforespoersel,
                       ordrenr,
                       ordrelinje,
                       delordrelinje,
                       artikkelnr,
                       antall,
                       produktgruppe,
                       data,
                       created,
                       oebs_id,
                       enhet,
                       produktgruppenr,
                       hjelpemiddeltype
                FROM v1_oebs_data
                WHERE soknads_id = :soknadId
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) {
            Ordrelinje(
                søknadId = søknadId,
                oebsId = it.int("oebs_id"),
                fnrBruker = it.string("fnr_bruker"),
                serviceforespørsel = it.intOrNull("serviceforespoersel"),
                ordrenr = it.int("ordrenr"),
                ordrelinje = it.int("ordrelinje"),
                delordrelinje = it.int("delordrelinje"),
                artikkelnr = it.string("artikkelnr"),
                antall = it.double("antall"),
                enhet = it.string("enhet"),
                produktgruppe = it.string("produktgruppe"),
                produktgruppenr = it.string("produktgruppenr"),
                hjelpemiddeltype = it.string("hjelpemiddeltype"),
                data = it.json("data"),
            )
        }
    }
}
