package no.nav.hjelpemidler.soknad.modell

data class SoknadSjekkResultat(
    val kanVæreBestilling: Boolean,
    val kriterier: Kriterier,
    val metaInfo: MetaInfo,
    val version: String,
)

data class Kriterier(
    val alleHovedProdukterPåBestillingsOrdning: Boolean,
    val alleTilbehørPåBestillingsOrdning: Boolean,
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
    val signaturtypeErIkkeBrukerbekreftelse: Boolean,
    val ingenProdukterErAlleredeUtlevert: Boolean,
    val brukerErTilknyttetBydelIOslo: Boolean?,
    val harIngenBytter: Boolean,
)

data class MetaInfo(
    val hovedProdukter: List<String>,
    val hovedProdukterIkkePåBestillingsordning: List<String>,
    val tilbehør: List<String>,
    val tilbehørIkkePåBestillingsordning: List<String>,
)
