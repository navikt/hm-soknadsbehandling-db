package no.nav.hjelpemidler.soknad.db.domain

data class ForslagsmotorTilbehoer(
    val hjelpemidler: Array<ForslagsmotorTilbehoerHjelpemiddel>,
)

data class ForslagsmotorTilbehoerHjelpemiddel(
    val hmsNr: String,
    val tilbehorListe: Array<ForslagsmotorTilbehoerTilbehoer>,
)

data class ForslagsmotorTilbehoerTilbehoer(
    val hmsnr: String,
    val antall: Int,
    val navn: String,
)
