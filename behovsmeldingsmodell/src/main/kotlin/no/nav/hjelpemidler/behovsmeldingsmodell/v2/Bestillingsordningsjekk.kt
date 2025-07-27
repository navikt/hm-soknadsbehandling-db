package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import com.fasterxml.jackson.annotation.JsonProperty

data class Bestillingsordningsjekk(
    val kanVæreBestilling: Boolean,
    val kriterier: Kriterier,
    val metaInfo: MetaInfo,
    val version: String,
) {
    data class Kriterier(
        @JsonProperty("alleHovedProdukterPåBestillingsOrdning")
        val alleHovedprodukterPåBestillingsordning: Boolean,
        @JsonProperty("alleTilbehørPåBestillingsOrdning")
        val alleTilbehørPåBestillingsordning: Boolean,
        val brukerHarHjelpemidlerFraFør: Boolean? = null,
        val brukerHarInfotrygdVedtakFraFør: Boolean? = null,
        val brukerHarHotsakVedtakFraFør: Boolean? = null,
        val leveringTilFolkeregistrertAdresse: Boolean,
        val brukersAdresseErSatt: Boolean,
        val brukerBorIkkeIUtlandet: Boolean,
        val brukerErIkkeSkjermetPerson: Boolean,
        val inneholderIkkeFritekst: Boolean,
        val kildeErPdl: Boolean,
        val harIkkeForMangeOrdrelinjer: Boolean,
        val ingenProdukterErAlleredeUtlevert: Boolean,
        val brukerErTilknyttetBydelIOslo: Boolean?,
        val harIngenBytter: Boolean,
        val brukerHarAdresseIOeBS: Boolean,
    )

    data class MetaInfo(
        @JsonProperty("hovedProdukter")
        val hovedprodukter: List<String>,
        @JsonProperty("hovedProdukterIkkePåBestillingsordning")
        val hovedprodukterIkkePåBestillingsordning: List<String>,
        val tilbehør: List<String>,
        val tilbehørIkkePåBestillingsordning: List<String>,
    )
}
