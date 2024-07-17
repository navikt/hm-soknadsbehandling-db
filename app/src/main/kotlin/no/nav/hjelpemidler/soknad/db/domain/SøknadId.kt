package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.database.Row
import java.util.UUID

typealias SøknadId = UUID

fun Row.søknadId(columnLabel: String = "soknads_id"): SøknadId = uuid(columnLabel)
