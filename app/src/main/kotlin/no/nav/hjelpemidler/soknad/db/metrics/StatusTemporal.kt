package no.nav.hjelpemidler.soknad.db.metrics

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import java.time.Instant
import java.time.temporal.Temporal

data class StatusTemporal(
    val status: BehovsmeldingStatus,
    val opprettet: Instant,
    val digital: Boolean,
) : Temporal by opprettet
