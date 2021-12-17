package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.databind.JsonNode
import java.util.Date
import java.util.UUID

class SoknadMedStatus private constructor(
    val soknadId: UUID,
    val journalpostId: String?,
    val datoOpprettet: Date,
    val datoOppdatert: Date,
    val status: Status,
    val fullmakt: Boolean,
    val formidlerNavn: String?,
    val er_digital: Boolean,
    val soknadGjelder: String?,
) {
    companion object {
        fun newSøknadUtenFormidlernavn(soknadId: UUID, journalpostId: String?, datoOpprettet: Date, datoOppdatert: Date, status: Status, fullmakt: Boolean, er_digital: Boolean, soknadGjelder: String?) =
            SoknadMedStatus(soknadId, journalpostId, datoOpprettet, datoOppdatert, status, fullmakt, null, er_digital, soknadGjelder)

        fun newSøknadMedFormidlernavn(soknadId: UUID, journalpostId: String?, datoOpprettet: Date, datoOppdatert: Date, status: Status, fullmakt: Boolean, søknad: JsonNode, er_digital: Boolean, soknadGjelder: String?) =
            SoknadMedStatus(soknadId, journalpostId, datoOpprettet, datoOppdatert, status, fullmakt, formidlerNavn(søknad), er_digital, soknadGjelder)
    }
}

private fun formidlerNavn(soknad: JsonNode): String {
    val leveringNode = soknad["soknad"]["levering"]
    return "${leveringNode["hmfFornavn"].textValue()} ${leveringNode["hmfEtternavn"].textValue()}"
}
