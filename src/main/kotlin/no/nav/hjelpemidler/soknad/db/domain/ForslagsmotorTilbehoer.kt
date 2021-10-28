package no.nav.hjelpemidler.soknad.db.domain

import java.util.UUID

data class ForslagsmotorTilbehoer_Soknad(
    val soknad: ForslagsmotorTilbehoer_Hjelpemidler,
)

data class ForslagsmotorTilbehoer_Hjelpemidler(
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
