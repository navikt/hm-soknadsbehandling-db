package no.nav.hjelpemidler.soknad.db.store

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSakId
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.database.Store
import java.time.LocalDate
import java.util.UUID

class HotsakStore(private val tx: JdbcOperations) : Store {
    fun lagKnytningMellomSakOgSøknad(søknadId: BehovsmeldingId, sakId: HotsakSakId): Int =
        tx.update(
            """
                INSERT INTO v1_hotsak_data (soknads_id, saksnummer)
                VALUES (:soknadId, :sakId)
                ON CONFLICT DO NOTHING
            """.trimIndent(),
            mapOf(
                "soknadId" to søknadId,
                "sakId" to sakId,
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

    fun finnSak(søknadId: BehovsmeldingId): HotsakSak? {
        return tx.singleOrNull(
            """
                SELECT soknads_id, saksnummer, vedtaksresultat, vedtaksdato, created
                FROM v1_hotsak_data
                WHERE soknads_id = :soknadId
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
            Row::tilHotsakSak,
        )
    }

    fun finnSak(sakId: HotsakSakId): HotsakSak? {
        return tx.singleOrNull(
            """
                SELECT soknads_id, saksnummer, vedtaksresultat, vedtaksdato, created
                FROM v1_hotsak_data
                WHERE saksnummer = :sakId
            """.trimIndent(),
            mapOf("sakId" to sakId.toString()),
            Row::tilHotsakSak,
        )
    }
}
