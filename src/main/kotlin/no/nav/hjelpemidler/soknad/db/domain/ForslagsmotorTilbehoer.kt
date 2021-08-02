package no.nav.hjelpemidler.soknad.db.domain

data class ForslagsmotorTilbehoerHjelpemiddel(
    val hmsNr: String,
    val tilbehorListe: List<ForslagsmotorTilbehoerTilbehoer>,
)

data class ForslagsmotorTilbehoerTilbehoer(
    val hmsnr: String,
    val antall: Int,
    val navn: String,
)
