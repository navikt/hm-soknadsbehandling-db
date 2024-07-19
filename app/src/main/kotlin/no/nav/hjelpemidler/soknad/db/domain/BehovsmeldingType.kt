package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.database.enumOrNull

// fixme -> tilBehovsmeldingType
fun Row.behovsmeldingType(columnLabel: String = "behovsmeldingType"): BehovsmeldingType =
    enumOrNull<BehovsmeldingType>(columnLabel) ?: BehovsmeldingType.SØKNAD
