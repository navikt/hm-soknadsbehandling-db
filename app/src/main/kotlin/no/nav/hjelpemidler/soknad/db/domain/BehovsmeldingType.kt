package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.database.enumOrNull

enum class BehovsmeldingType {
    SØKNAD,
    BESTILLING,
    BYTTE,
    BRUKERPASSBYTTE,
}

fun Row.behovsmeldingType(columnLabel: String = "behovsmeldingType"): BehovsmeldingType =
    enumOrNull<BehovsmeldingType>(columnLabel) ?: BehovsmeldingType.SØKNAD
