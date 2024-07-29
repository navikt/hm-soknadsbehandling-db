package no.nav.hjelpemidler.soknad.db.sak

import kotliquery.Row
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Vedtak

fun Row.tilVedtak(): Vedtak? = stringOrNull("vedtaksresultat")?.let {
    Vedtak(vedtaksresultat = it, vedtaksdato = localDate("vedtaksdato"))
}
