package no.nav.hjelpemidler.soknad.db.domain

import java.time.LocalDateTime
import java.util.UUID

data class ForslagsmotorTilbehoer_Hjelpemidler(
    val soknad: ForslagsmotorTilbehoer_Soknad,
    val created: LocalDateTime,
)

data class ForslagsmotorTilbehoer_Soknad(
    val id: UUID,
    val hjelpemidler: ForslagsmotorTilbehoer_HjelpemiddelListe,
)

data class ForslagsmotorTilbehoer_HjelpemiddelListe(
    val hjelpemiddelListe: Array<ForslagsmotorTilbehoer_Hjelpemiddel>
)

data class ForslagsmotorTilbehoer_Hjelpemiddel(
    val hmsNr: String,
    val tilbehorListe: Array<ForslagsmotorTilbehoer_Tilbehoer>?,
)

data class ForslagsmotorTilbehoer_Tilbehoer(
    val hmsnr: String,
    val antall: Int,
    val navn: String,
)
