package no.nav.hjelpemidler.soknad.db.exception

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus

/**
 * Kastes når en behovsmelding ikke finnes i databasen.
 */
class BehovsmeldingNotFoundException(behovsmeldingId: BehovsmeldingId) : RuntimeException("Behovsmelding med id $behovsmeldingId ble ikke funnet")

/**
 * Kastes når en behovsmelding har feil status for en operasjon.
 */
class BehovsmeldingUgyldigStatusException(
    behovsmeldingId: BehovsmeldingId,
    nåværendeStatus: BehovsmeldingStatus,
    forventetStatus: BehovsmeldingStatus,
) : RuntimeException(
    "Behovsmelding $behovsmeldingId har ugyldig status $nåværendeStatus. Forventet status: $forventetStatus",
)
