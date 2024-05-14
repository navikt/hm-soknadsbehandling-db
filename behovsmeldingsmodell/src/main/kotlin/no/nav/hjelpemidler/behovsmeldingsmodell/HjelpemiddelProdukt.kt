package no.nav.hjelpemidler.behovsmeldingsmodell

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

data class HjelpemiddelProdukt(
    /**
     * Hmsnr.
     */
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
    @JsonProperty("techdata")
    @JsonAlias("technicalData", "techdata", "Techdata")
    var technicalData: List<TechnicalDatum> = emptyList(),
    @JsonProperty("techdataAsText")
    var technicalDataAsText: String?,
    @JsonProperty("paakrevdGodkjenningskurs")
    var påkrevdGodkjenningskurs: PåkrevdGodkjenningskurs?,
) {
    data class TechnicalDatum(
        @JsonProperty("techlabeldk")
        val label: String?,
        @JsonProperty("datavalue")
        val value: String?,
        @JsonProperty("techdataunit")
        val unit: String?,
    )

    data class PåkrevdGodkjenningskurs(
        val kursId: String?,
        val tittel: String?,
        val isokode: String?,
        @JsonProperty("formidlersGjennomforing")
        val formidlersGjennomføring: FormidlersGjennomføringAvKurs?,
    )

    enum class FormidlersGjennomføringAvKurs {
        GODKJENNINGSKURS_DB,
        VALGT_AV_FORMIDLER,
    }
}
