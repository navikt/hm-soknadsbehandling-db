package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import java.sql.Timestamp
import java.util.UUID

data class StatusMedÅrsak(
    val søknadId: UUID,
    val status: BehovsmeldingStatus,
    val valgteÅrsaker: Set<String>?,
    val begrunnelse: String?,
)

data class StatusCountRow(val STATUS: BehovsmeldingStatus, val COUNT: Number)

data class StatusRow(
    val STATUS: BehovsmeldingStatus,
    val CREATED: Timestamp,
    val ER_DIGITAL: Boolean,
)
