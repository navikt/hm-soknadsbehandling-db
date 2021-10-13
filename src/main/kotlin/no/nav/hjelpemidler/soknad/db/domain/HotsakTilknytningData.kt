package no.nav.hjelpemidler.soknad.db.domain

import java.util.UUID

internal data class HotsakTilknytningData(
    val søknadId: UUID,
    val saksnr: String
)
