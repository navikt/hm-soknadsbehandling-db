package no.nav.hjelpemidler.soknad.db.store

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.Store
import no.nav.hjelpemidler.time.toLocalDate
import no.nav.hjelpemidler.time.toLocalDateTime
import java.time.Clock
import java.time.Instant

class BrukerbekreftelseVarselStore(private val tx: JdbcOperations, private val clock: Clock) : Store {

    private fun nå() = Instant.now(clock)

    fun lagreEpostvarsling(epost: String): Long = tx.updateAndReturnGeneratedKey(
        """
                INSERT INTO v1_varsel_brukerbekreftelse (formidler_epost, tidspunkt_varslet)
                VALUES (:epost, :now)
        """.trimIndent(),
        mapOf(
            "epost" to epost,
            "now" to nå(),
        ),
    )

    fun hentVarslerForIDag(): List<BrukerbekreftelseVarselEntity> {
        return tx.list(
            """
                SELECT id, formidler_epost
                FROM v1_varsel_brukerbekreftelse
                WHERE tidspunkt_varslet >= :iDag
            """.trimIndent(),
            mapOf("iDag" to nå().toLocalDate()),
            { row ->
                BrukerbekreftelseVarselEntity(
                    id = row.long("id"),
                    formidlersEpost = row.string("formidler_epost"),
                )
            },
        )
    }

    fun lagreVarsletBehovsmelding(varselId: Long, behovsmeldingId: BehovsmeldingId): Int = tx.update(
        """
                INSERT INTO v1_varsel_brukerbekreftelse_behovsmelding (varsel_id, behovsmelding_id)
                VALUES (:varselId, :behovsmeldingId)
        """.trimIndent(),
        mapOf(
            "varselId" to varselId,
            "behovsmeldingId" to behovsmeldingId,
        ),
    ).actualRowCount

    fun hentSisteVarsling(behovsmeldingId: BehovsmeldingId): Instant? {
        return tx.singleOrNull(
            """
                SELECT vb.tidspunkt_varslet
                FROM v1_varsel_brukerbekreftelse vb
                JOIN v1_varsel_brukerbekreftelse_behovsmelding vbb 
                    ON vb.id = vbb.varsel_id
                WHERE vbb.behovsmelding_id = :behovsmeldingId
                ORDER BY vb.tidspunkt_varslet DESC 
                LIMIT 1
            """.trimIndent(),
            mapOf(
                "behovsmeldingId" to behovsmeldingId,
            ),
            { row ->
                row.instant("tidspunkt_varslet")
            },
        )
    }
}

data class BrukerbekreftelseVarselEntity(
    val id: Long,
    val formidlersEpost: String,
)
