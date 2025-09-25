package no.nav.hjelpemidler.soknad.db.safselvbetjening

import com.fasterxml.jackson.databind.JsonNode

data class GraphqlRequest(
    val query: String,
    val variables: Map<String, String>,
)

data class GraphqlResult(
    val data: Data?,
    val errors: JsonNode?,
)

data class Data(
    val dokumentoversiktSelvbetjening: DokumentoversiktSelvbetjening,
)

data class DokumentoversiktSelvbetjening(
    val tema: List<Tema>,
)

data class Tema(
    val journalposter: List<Journalpost>,
)

data class Journalpost(
    val journalpostId: String,
    val tittel: String?,
    val journalposttype: String,
    val journalstatus: String?,
    val sak: Sak?,
    val kanal: String?,
    val relevanteDatoer: List<RelevanteDato>,
    val dokumenter: List<DokumentInfo>?,
) {
    // Brukt i frontenden, men ikke fra safselvbetjening datamodellen
    val dato get() = relevanteDatoer.find { it.datotype == "DATO_OPPRETTET" }?.dato
}

data class RelevanteDato(
    val dato: String,
    val datotype: String,
)

data class Sak(
    val fagsaksystem: String?,
    val fagsakId: String?,
)

data class DokumentInfo(
    val tittel: String?,
    val dokumentInfoId: String,
    val brevkode: String?,
    val dokumentvarianter: List<Dokumentvariant>,
)

data class Dokumentvariant(
    val variantformat: String,
    val brukerHarTilgang: String,
    val code: List<String>,
)
