package no.nav.hjelpemidler.soknad.db.domain

import java.time.LocalDateTime
import java.util.UUID

data class ForslagsmotorTilbehørHjelpemidler(
    val soknad: ForslagsmotorTilbehørSøknad,
    var created: LocalDateTime? = null,
)

data class ForslagsmotorTilbehørSøknad(
    val id: UUID,
    val hjelpemidler: ForslagsmotorTilbehørHjelpemiddelListe,
)

data class ForslagsmotorTilbehørHjelpemiddelListe(
    val hjelpemiddelListe: List<ForslagsmotorTilbehørHjelpemiddel>,
)

data class ForslagsmotorTilbehørHjelpemiddel(
    val hmsNr: String,
    val tilbehorListe: List<ForslagsmotorTilbehørTilbehør>?,
)

data class ForslagsmotorTilbehørTilbehør(
    val hmsnr: String,
    val antall: Int,
    val navn: String,
)
