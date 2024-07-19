package no.nav.hjelpemidler.behovsmeldingsmodell.sak

import java.time.LocalDate

data class Vedtak(
    val vedtaksresultat: String,
    val vedtaksdato: LocalDate,
)
