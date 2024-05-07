package no.nav.hjelpemidler.behovsmeldingsmodell

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

data class HjelpemiddelProdukt(
    val stockid: String?,
    val artid: String?,
    val prodid: String?,
    val artno: String?,
    val artname: String?,
    val adescshort: String?,
    val prodname: String?,
    val pshortdesc: String?,
    val artpostid: String?,
    val apostid: String?,
    val postrank: String?,
    val apostnr: String?,
    val aposttitle: String?,
    val newsid: String?,
    val isocode: String?,
    val isotitle: String?,
    var kategori: String?,
    @JsonAlias("technicalData", "techdata", "Techdata")
    var techdata: List<Techdata>? = emptyList(),
    var techdataAsText: String?,
    @JsonProperty("paakrevdGodkjenningskurs")
    var påkrevdGodkjenningskurs: PåkrevdGodkjenningskurs?,
)

data class Techdata(
    val techlabeldk: String?,
    val datavalue: String?,
    val techdataunit: String?,
)

data class PåkrevdGodkjenningskurs(
    val kursId: Int?,
    val tittel: String?,
    val isokode: String?,
    @JsonProperty("formidlersGjennomforing")
    val formidlersGjennomføring: FormidlersGjennomføringAvKurs?,
)

enum class FormidlersGjennomføringAvKurs {
    GODKJENNINGSKURS_DB,
    VALGT_AV_FORMIDLER,
}
