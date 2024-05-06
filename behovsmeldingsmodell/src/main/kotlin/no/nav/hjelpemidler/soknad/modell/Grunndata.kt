package no.nav.hjelpemidler.soknad.modell

import com.fasterxml.jackson.annotation.JsonAlias

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
    var techdata: Array<Techdata>? = emptyArray(),
    var techdataAsText: String?,
    var paakrevdGodkjenningskurs: PaakrevdGodkjenningsKurs?,
)

data class Techdata(
    val techlabeldk: String?,
    val datavalue: String?,
    val techdataunit: String?,
)

data class PaakrevdGodkjenningsKurs(
    val kursId: Int?,
    val tittel: String?,
    val isokode: String?,
    val formidlersGjennomforing: FormidlersGjennomforingAvKurs?,
)

enum class FormidlersGjennomforingAvKurs {
    GODKJENNINGSKURS_DB,
    VALGT_AV_FORMIDLER,
}
