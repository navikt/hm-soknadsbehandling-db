package no.nav.hjelpemidler.soknad.db.sak

import kotliquery.Row
import java.time.LocalDate

data class Vedtak(
    val vedtaksresultat: String,
    val vedtaksdato: LocalDate,
)

fun Row.tilVedtak(): Vedtak? = stringOrNull("vedtaksresultat")?.let {
    Vedtak(vedtaksresultat = it, vedtaksdato = localDate("vedtaksdato"))
}
