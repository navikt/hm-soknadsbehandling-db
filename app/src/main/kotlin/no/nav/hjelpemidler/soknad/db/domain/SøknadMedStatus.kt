package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.databind.JsonNode
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import java.util.Date
import java.util.UUID

class SøknadMedStatus private constructor(
    val soknadId: BehovsmeldingId,
    val behovsmeldingType: BehovsmeldingType,
    val journalpostId: String?,
    val datoOpprettet: Date,
    val datoOppdatert: Date,
    val status: BehovsmeldingStatus,
    val fullmakt: Boolean,
    val formidlerNavn: String?,
    val er_digital: Boolean,
    val soknadGjelder: String?,
    val valgteÅrsaker: List<String>,
) {
    companion object {
        fun newSøknadUtenFormidlernavn(
            soknadId: UUID,
            behovsmeldingType: BehovsmeldingType,
            journalpostId: String?,
            datoOpprettet: Date,
            datoOppdatert: Date,
            status: BehovsmeldingStatus,
            fullmakt: Boolean,
            er_digital: Boolean,
            soknadGjelder: String?,
            valgteÅrsaker: List<String>,
        ) = SøknadMedStatus(
            soknadId,
            behovsmeldingType,
            journalpostId,
            datoOpprettet,
            datoOppdatert,
            status,
            fullmakt,
            null,
            er_digital,
            soknadGjelder,
            valgteÅrsaker,
        )

        fun newSøknadMedFormidlernavn(
            soknadId: UUID,
            behovsmeldingType: BehovsmeldingType,
            journalpostId: String?,
            datoOpprettet: Date,
            datoOppdatert: Date,
            status: BehovsmeldingStatus,
            fullmakt: Boolean,
            søknad: JsonNode,
            er_digital: Boolean,
            soknadGjelder: String?,
            valgteÅrsaker: List<String>,
        ) = SøknadMedStatus(
            soknadId,
            behovsmeldingType,
            journalpostId,
            datoOpprettet,
            datoOppdatert,
            status,
            fullmakt,
            formidlerNavn(søknad, behovsmeldingType),
            er_digital,
            soknadGjelder,
            valgteÅrsaker,
        )
    }
}

private fun formidlerNavn(
    soknad: JsonNode,
    behovsmeldingType: BehovsmeldingType,
): String? {
    if (behovsmeldingType == BehovsmeldingType.BRUKERPASSBYTTE) {
        return null
    }
    val leveringNode = soknad["soknad"]["levering"]
    return "${leveringNode["hmfFornavn"].textValue()} ${leveringNode["hmfEtternavn"].textValue()}"
}
