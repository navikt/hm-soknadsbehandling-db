package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.databind.JsonNode
import java.util.Date
import java.util.UUID

class SoknadMedStatus private constructor(
    val soknadId: UUID,
    val behovsmeldingType: BehovsmeldingType,
    val journalpostId: String?,
    val datoOpprettet: Date,
    val datoOppdatert: Date,
    val status: Status,
    val fullmakt: Boolean,
    val formidlerNavn: String?,
    val er_digital: Boolean,
    val soknadGjelder: String?,
    val valgteÅrsaker: List<String>,
) {
    companion object {
        fun newSøknadUtenFormidlernavn(soknadId: UUID, behovsmeldingType: BehovsmeldingType, journalpostId: String?, datoOpprettet: Date, datoOppdatert: Date, status: Status, fullmakt: Boolean, er_digital: Boolean, soknadGjelder: String?, valgteÅrsaker: List<String>) =
            SoknadMedStatus(soknadId, behovsmeldingType, journalpostId, datoOpprettet, datoOppdatert, status, fullmakt, null, er_digital, soknadGjelder, valgteÅrsaker)

        fun newSøknadMedFormidlernavn(soknadId: UUID, behovsmeldingType: BehovsmeldingType, journalpostId: String?, datoOpprettet: Date, datoOppdatert: Date, status: Status, fullmakt: Boolean, søknad: JsonNode, er_digital: Boolean, soknadGjelder: String?, valgteÅrsaker: List<String>) =
            SoknadMedStatus(soknadId, behovsmeldingType, journalpostId, datoOpprettet, datoOppdatert, status, fullmakt, formidlerNavn(søknad), er_digital, soknadGjelder, valgteÅrsaker)
    }
}

private fun formidlerNavn(soknad: JsonNode): String {
    val leveringNode = soknad["soknad"]["levering"]
    return "${leveringNode["hmfFornavn"].textValue()} ${leveringNode["hmfEtternavn"].textValue()}"
}
