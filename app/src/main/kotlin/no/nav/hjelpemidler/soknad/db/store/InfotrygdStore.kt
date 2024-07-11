package no.nav.hjelpemidler.soknad.db.store

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.soknad.db.domain.FagsakData
import no.nav.hjelpemidler.soknad.db.domain.VedtaksresultatData
import java.time.LocalDate
import java.util.UUID

private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class InfotrygdStore(private val tx: JdbcOperations) {
    // EndeligJournalført frå Joark vil opprette linja, og denne blir berika seinare av Infotrygd med resultat og vedtaksdato
    fun lagKnytningMellomFagsakOgSøknad(vedtaksresultatData: VedtaksresultatData): Int =
        tx.update(
            """
                INSERT INTO v1_infotrygd_data (soknads_id, fnr_bruker, trygdekontornr, saksblokk, saksnr)
                VALUES (:soknadId, :fnrBruker, :trygdekontorNr, :saksblokk, :saksnr)
                ON CONFLICT DO NOTHING
            """.trimIndent(),
            mapOf(
                "soknadId" to vedtaksresultatData.søknadId,
                "fnrBruker" to vedtaksresultatData.fnrBruker,
                "trygdekontorNr" to vedtaksresultatData.trygdekontorNr,
                "saksblokk" to vedtaksresultatData.saksblokk,
                "saksnr" to vedtaksresultatData.saksnr,
            ),
        ).actualRowCount

    // Vedtaksresultat vil bli gitt av Infotrygd-poller som har oversikt over søknadId, fnr og fagsakId
    fun lagreVedtaksresultat(
        søknadId: UUID,
        vedtaksresultat: String,
        vedtaksdato: LocalDate,
        søknadstype: String,
    ): Int = tx.update(
        """
            UPDATE v1_infotrygd_data
            SET vedtaksresultat = :vedtaksresultat,
                vedtaksdato = :vedtaksdato,
                soknadstype = :soknadstype
            WHERE soknads_id = :soknadId
        """.trimIndent(),
        mapOf(
            "vedtaksresultat" to vedtaksresultat,
            "vedtaksdato" to vedtaksdato,
            "soknadstype" to søknadstype,
            "soknadId" to søknadId,
        ),
    ).actualRowCount

    // Brukt for å matche OEBS-data mot et Infotrygd-resultat
    fun hentSøknadIdFraVedtaksresultat(
        fnrBruker: String,
        saksblokkOgSaksnr: String,
        vedtaksdato: LocalDate,
    ): UUID? {
        val uuids: List<UUID> = tx.list(
            """
                SELECT soknads_id
                FROM v1_infotrygd_data
                WHERE fnr_bruker = :fnrBruker
                  AND saksblokk = :saksblokk
                  AND saksnr = :saksnr
                  AND vedtaksdato = :vedtaksdato
            """.trimIndent(),
            mapOf(
                "fnrBruker" to fnrBruker,
                "saksblokk" to saksblokkOgSaksnr.first(),
                "saksnr" to saksblokkOgSaksnr.takeLast(2),
                "vedtaksdato" to vedtaksdato,
            ),
        ) { it.uuid("soknads_id") }
        if (uuids.count() != 1) {
            if (uuids.count() > 1) {
                sikkerlogg.info { "Fant flere søknader med matchende fnr, saksnr og vedtaksdato, saksblokkOgSaksnr: $saksblokkOgSaksnr, vedtaksdato: $vedtaksdato, antall: ${uuids.count()}, ids: [$uuids]" }
            } else {
                sikkerlogg.info { "Kan ikke knytte ordrelinje til søknad. saksblokkOgSaksnr: $saksblokkOgSaksnr, vedtaksdato: $vedtaksdato" }
            }
            return null
        }
        return uuids[0]
    }

    // Brukt for å matche Oebs-data mot eit Infotrygd-resultat i alternativ flyt
    fun hentSøknadIdFraVedtaksresultatV2(
        fnrBruker: String,
        saksblokkOgSaksnr: String,
    ): List<SøknadIdFraVedtaksresultat> {
        return tx.list(
            """
                SELECT soknads_id, vedtaksdato
                FROM v1_infotrygd_data
                WHERE fnr_bruker = :fnrBruker
                  AND saksblokk = :saksblokk
                  AND saksnr = :saksnr
            """.trimIndent(),
            mapOf(
                "fnrBruker" to fnrBruker,
                "saksblokk" to saksblokkOgSaksnr.first(),
                "saksnr" to saksblokkOgSaksnr.takeLast(2),
            ),
        ) {
            SøknadIdFraVedtaksresultat(
                it.uuid("soknads_id"),
                it.localDateOrNull("vedtaksdato"),
            )
        }
    }

    data class SøknadIdFraVedtaksresultat(
        val søknadId: UUID,
        val vedtaksDato: LocalDate?,
    )

    fun hentVedtaksresultatForSøknad(søknadId: UUID): VedtaksresultatData? {
        return tx.singleOrNull(
            """
                SELECT soknads_id, fnr_bruker, trygdekontornr, saksblokk, saksnr, vedtaksresultat, vedtaksdato
                FROM v1_infotrygd_data
                WHERE soknads_id = :soknadId
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) {
            VedtaksresultatData(
                søknadId = it.uuid("SOKNADS_ID"),
                fnrBruker = it.string("FNR_BRUKER"),
                trygdekontorNr = it.string("TRYGDEKONTORNR"),
                saksblokk = it.string("SAKSBLOKK"),
                saksnr = it.string("SAKSNR"),
                vedtaksresultat = it.stringOrNull("VEDTAKSRESULTAT"),
                vedtaksdato = it.localDateOrNull("VEDTAKSDATO"),
            )
        }
    }

    fun hentFagsakIdForSøknad(søknadId: UUID): FagsakData? {
        return tx.singleOrNull(
            """
                SELECT soknads_id, trygdekontornr, saksblokk, saksnr
                FROM v1_infotrygd_data
                WHERE soknads_id = :soknadId
                  AND trygdekontornr IS NOT NULL
                  AND saksblokk IS NOT NULL
                  AND saksnr IS NOT NULL
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) {
            FagsakData(
                søknadId = it.uuid("SOKNADS_ID"),
                fagsakId = it.string("TRYGDEKONTORNR") + it.string("SAKSBLOKK") + it.string("SAKSNR"),
            )
        }
    }

    fun hentTypeForSøknad(søknadId: UUID): String? {
        return tx.singleOrNull(
            """
                SELECT soknadstype
                FROM v1_infotrygd_data
                WHERE soknads_id = :soknadId
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
        ) { it.stringOrNull("soknadstype") }
    }
}
