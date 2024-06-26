package no.nav.hjelpemidler.soknad.db.domain

import java.sql.Timestamp
import java.util.UUID

/**
 * @see [no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus]
 */
enum class Status {
    VENTER_GODKJENNING,
    GODKJENT_MED_FULLMAKT,
    INNSENDT_FULLMAKT_IKKE_PÅKREVD,
    BRUKERPASSBYTTE_INNSENDT,
    GODKJENT,
    SLETTET,
    UTLØPT,
    ENDELIG_JOURNALFØRT,
    BESTILLING_FERDIGSTILT,
    BESTILLING_AVVIST,
    VEDTAKSRESULTAT_INNVILGET,
    VEDTAKSRESULTAT_MUNTLIG_INNVILGET,
    VEDTAKSRESULTAT_DELVIS_INNVILGET,
    VEDTAKSRESULTAT_AVSLÅTT,
    VEDTAKSRESULTAT_ANNET,
    UTSENDING_STARTET,
    VEDTAKSRESULTAT_HENLAGTBORTFALT,
    ;

    fun isSlettetEllerUtløpt() = this == SLETTET || this == UTLØPT
}

data class StatusMedÅrsak(
    val søknadId: UUID,
    val status: Status,
    val valgteÅrsaker: Set<String>?,
    val begrunnelse: String?,
)

data class StatusCountRow(val STATUS: Status, val COUNT: Number)

data class StatusRow(val STATUS: Status, val CREATED: Timestamp, val ER_DIGITAL: Boolean)
