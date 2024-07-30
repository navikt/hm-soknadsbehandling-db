package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.database.enumOrNull

fun Row.tilBehovsmeldingType(columnLabel: String = "behovsmeldingType"): BehovsmeldingType =
    enumOrNull<BehovsmeldingType>(columnLabel) ?: BehovsmeldingType.SØKNAD
