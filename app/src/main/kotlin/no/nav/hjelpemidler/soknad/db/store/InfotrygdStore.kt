package no.nav.hjelpemidler.soknad.db.store

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSakId
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.database.Store
import no.nav.hjelpemidler.soknad.db.domain.SøknadId
import no.nav.hjelpemidler.soknad.db.sak.tilInfotrygdSak
import java.time.LocalDate
import java.util.UUID

private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class InfotrygdStore(private val tx: JdbcOperations) : Store {
    // EndeligJournalført frå Joark vil opprette linja, og denne blir berika seinare av Infotrygd med resultat og vedtaksdato
    fun lagKnytningMellomSakOgSøknad(søknadId: SøknadId, sakId: InfotrygdSakId, fnrBruker: String): Int =
        tx.update(
            """
                INSERT INTO v1_infotrygd_data (soknads_id, fnr_bruker, trygdekontornr, saksblokk, saksnr)
                VALUES (:soknadId, :fnrBruker, :trygdekontornummer, :saksblokk, :saksnummer)
                ON CONFLICT DO NOTHING
            """.trimIndent(),
            mapOf(
                "soknadId" to søknadId,
                "fnrBruker" to fnrBruker,
                "trygdekontornummer" to sakId.trygdekontornummer,
                "saksblokk" to sakId.saksblokk,
                "saksnummer" to sakId.saksnummer,
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
                vedtaksdato     = :vedtaksdato,
                soknadstype     = :soknadstype
            WHERE soknads_id = :soknadId
        """.trimIndent(),
        mapOf(
            "vedtaksresultat" to vedtaksresultat,
            "vedtaksdato" to vedtaksdato,
            "soknadstype" to søknadstype,
            "soknadId" to søknadId,
        ),
    ).actualRowCount

    fun finnSak(søknadId: SøknadId): InfotrygdSak? {
        return tx.singleOrNull(
            """
                SELECT soknads_id,
                       fnr_bruker,
                       trygdekontornr,
                       saksblokk,
                       saksnr,
                       vedtaksresultat,
                       vedtaksdato,
                       created,
                       soknadstype
                FROM v1_infotrygd_data
                WHERE soknads_id = :soknadId
            """.trimIndent(),
            mapOf("soknadId" to søknadId),
            Row::tilInfotrygdSak,
        )
    }

    // Brukt for å matche OEBS-data mot et Infotrygd-resultat
    fun hentSøknadIdFraVedtaksresultatV1(
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

    // Brukt for å matche OEBS-data mot et Infotrygd-resultat i alternativ flyt
    fun hentSøknadIdFraVedtaksresultatV2(
        fnrBruker: String,
        saksblokkOgSaksnr: String,
    ): List<InfotrygdSak> {
        return tx.list(
            """
                SELECT soknads_id,
                       fnr_bruker,
                       trygdekontornr,
                       saksblokk,
                       saksnr,
                       vedtaksresultat,
                       vedtaksdato,
                       created,
                       soknadstype
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
            Row::tilInfotrygdSak,
        )
    }
}
