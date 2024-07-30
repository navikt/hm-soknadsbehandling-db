package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadId
import no.nav.hjelpemidler.database.Row

fun Row.tilSøknadId(columnLabel: String = "soknads_id"): SøknadId =
    uuid(columnLabel)
