package no.nav.hjelpemidler.soknad.db.store

import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.Store
import no.nav.hjelpemidler.soknad.db.domain.HotsakTilknytningData
import no.nav.hjelpemidler.soknad.db.domain.VedtaksresultatHotsakData
import java.time.LocalDate
import java.util.UUID

class HotsakStore(private val tx: JdbcOperations) : Store {
    fun lagKnytningMellomSakOgSøknad(hotsakTilknytningData: HotsakTilknytningData): Int =
        tx.update(
            """
                INSERT INTO v1_hotsak_data (soknads_id, saksnummer)
                VALUES (:soknadId, :saksnummer)
                ON CONFLICT DO NOTHING
            """.trimIndent(),
            mapOf(
                "soknadId" to hotsakTilknytningData.søknadId,
                "saksnummer" to hotsakTilknytningData.saksnr,
            ),
        ).actualRowCount

    fun lagreVedtaksresultat(
        søknadId: UUID,
        vedtaksresultat: String,
        vedtaksdato: LocalDate,
    ): Int = tx.update(
        """
            UPDATE v1_hotsak_data
            SET vedtaksresultat = :vedtaksresultat,
                vedtaksdato     = :vedtaksdato
            WHERE soknads_id = :soknadId
        """.trimIndent(),
        mapOf(
            "vedtaksresultat" to vedtaksresultat,
            "vedtaksdato" to vedtaksdato,
            "soknadId" to søknadId,
        ),
    ).actualRowCount

    fun hentVedtaksresultatForSøknad(søknadId: UUID): VedtaksresultatHotsakData? {
        return tx.singleOrNull(
            """
                SELECT soknads_id, saksnummer, vedtaksresultat, vedtaksdato
                FROM v1_hotsak_data
                WHERE soknads_id = :soknadId
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) {
            VedtaksresultatHotsakData(
                søknadId = it.uuid("soknads_id"),
                saksnr = it.string("saksnummer"),
                vedtaksresultat = it.stringOrNull("vedtaksresultat"),
                vedtaksdato = it.localDateOrNull("vedtaksdato"),
            )
        }
    }

    fun finnSøknadIdForSak(saksnummer: String): UUID? =
        tx.singleOrNull(
            """
                SELECT soknads_id
                FROM v1_hotsak_data
                WHERE saksnummer = :saksnummer
            """.trimIndent(),
            mapOf("saksnummer" to saksnummer),
        ) { it.uuid("soknads_id") }

    fun finnSaksnummerForSøknad(søknadId: UUID): String? =
        tx.singleOrNull(
            """
                SELECT saksnummer
                FROM v1_hotsak_data
                WHERE soknads_id = :soknadId
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) { it.string("saksnummer") }

    fun harVedtakForSøknadId(søknadId: UUID): Boolean =
        tx.singleOrNull(
            """
                SELECT 1
                FROM v1_hotsak_data
                WHERE soknads_id = :soknadId
                  AND vedtaksresultat IS NOT NULL
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) { true } ?: false
}
