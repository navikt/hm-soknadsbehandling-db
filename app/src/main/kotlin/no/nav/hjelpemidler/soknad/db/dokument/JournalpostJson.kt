package no.nav.hjelpemidler.soknad.db.dokument

import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.datoOpprettet
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.enums.Datotype
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.enums.Journalposttype
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.enums.Journalstatus
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.enums.Kanal
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.enums.Variantformat
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.hentdokumentoversiktselvbetjening.Journalpost
import java.time.LocalDateTime

data class JournalpostJson(
    val journalpostId: String,
    val tittel: String?,
    val journalposttype: Journalposttype,
    val journalstatus: Journalstatus?,
    val sak: Sak?,
    val kanal: Kanal?,
    val relevanteDatoer: List<RelevantDato>,
    val dokumenter: List<DokumentInfo>?,
    val dato: LocalDateTime?,
) {
    constructor(journalpost: Journalpost) : this(
        journalpostId = journalpost.journalpostId,
        tittel = journalpost.tittel,
        journalposttype = journalpost.journalposttype,
        journalstatus = journalpost.journalstatus,
        sak = journalpost.sak?.let { Sak(fagsakId = it.fagsakId, fagsaksystem = it.fagsaksystem) },
        kanal = journalpost.kanal,
        relevanteDatoer = journalpost.relevanteDatoer.filterNotNull()
            .map { RelevantDato(dato = it.dato.toLocalDateTime(), datotype = it.datotype) },
        dokumenter = journalpost.dokumenter?.filterNotNull()?.map { dokument ->
            DokumentInfo(
                dokumentInfoId = dokument.dokumentInfoId,
                tittel = dokument.tittel,
                brevkode = dokument.brevkode,
                dokumentvarianter = dokument.dokumentvarianter.filterNotNull().map {
                    Dokumentvariant(
                        variantformat = it.variantformat,
                        brukerHarTilgang = it.brukerHarTilgang,
                        code = it.code.filterNotNull(),
                    )
                },
            )
        },
        dato = journalpost.datoOpprettet?.toLocalDateTime(),
    )

    data class RelevantDato(
        val dato: LocalDateTime,
        val datotype: Datotype,
    )

    data class Sak(
        val fagsakId: String?,
        val fagsaksystem: String?,
    )

    data class DokumentInfo(
        val dokumentInfoId: String,
        val tittel: String?,
        val brevkode: String?,
        val dokumentvarianter: List<Dokumentvariant>,
    )

    data class Dokumentvariant(
        val variantformat: Variantformat,
        val brukerHarTilgang: Boolean,
        val code: List<String>,
    )
}
