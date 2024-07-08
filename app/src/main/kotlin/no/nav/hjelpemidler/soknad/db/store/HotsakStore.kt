package no.nav.hjelpemidler.soknad.db.store

import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.soknad.db.domain.HotsakTilknytningData
import no.nav.hjelpemidler.soknad.db.domain.VedtaksresultatHotsakData
import java.time.LocalDate
import java.util.UUID

interface HotsakStore {
    fun lagKnytningMellomSakOgSøknad(hotsakTilknytningData: HotsakTilknytningData): Int
    fun lagreVedtaksresultat(
        søknadId: UUID,
        vedtaksresultat: String,
        vedtaksdato: LocalDate,
    ): Int

    fun hentVedtaksresultatForSøknad(søknadId: UUID): VedtaksresultatHotsakData?
    fun hentSøknadsIdForHotsakNummer(saksnummer: String): UUID?
    fun harVedtakForSøknadId(søknadId: UUID): Boolean
    fun hentFagsakIdForSøknad(søknadId: UUID): String?
}

class HotsakStorePostgres(private val tx: JdbcOperations) : HotsakStore {
    override fun lagKnytningMellomSakOgSøknad(hotsakTilknytningData: HotsakTilknytningData): Int =
        time("insert_knytning_mellom_søknad_og_hotsak") {
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
        }

    override fun lagreVedtaksresultat(
        søknadId: UUID,
        vedtaksresultat: String,
        vedtaksdato: LocalDate,
    ): Int =
        time("oppdater_vedtaksresultat_fra_hotsak") {
            tx.update(
                """
                    UPDATE v1_hotsak_data
                    SET vedtaksresultat = :vedtaksresultat,
                        vedtaksdato = :vedtaksdato
                    WHERE soknads_id = :soknadId
                """.trimIndent(),
                mapOf(
                    "vedtaksresultat" to vedtaksresultat,
                    "vedtaksdato" to vedtaksdato,
                    "soknadId" to søknadId,
                ),
            ).actualRowCount
        }

    override fun hentVedtaksresultatForSøknad(søknadId: UUID): VedtaksresultatHotsakData? {
        return time("hent_søknadid_fra_resultat") {
            tx.singleOrNull(
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
    }

    override fun hentSøknadsIdForHotsakNummer(saksnummer: String): UUID? =
        tx.singleOrNull(
            "SELECT soknads_id FROM v1_hotsak_data WHERE saksnummer = :saksnummer",
            mapOf("saksnummer" to saksnummer),
        ) { it.uuid("soknads_id") }

    override fun harVedtakForSøknadId(søknadId: UUID): Boolean =
        tx.singleOrNull(
            "SELECT 1 FROM v1_hotsak_data WHERE soknads_id = :soknadId AND vedtaksresultat IS NOT NULL",
            mapOf("soknadId" to søknadId),
        ) { true } ?: false

    override fun hentFagsakIdForSøknad(søknadId: UUID): String? =
        tx.singleOrNull(
            "SELECT saksnummer FROM v1_hotsak_data WHERE soknads_id = :soknadId",
            mapOf("soknadId" to søknadId),
        ) { it.string("saksnummer") }
}
