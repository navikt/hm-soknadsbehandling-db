package no.nav.hjelpemidler.soknad.db.sak

import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Vedtak
import no.nav.hjelpemidler.database.Row

fun Row.tilVedtak(): Vedtak? = stringOrNull("vedtaksresultat")?.let {
    Vedtak(vedtaksresultat = it, vedtaksdato = localDate("vedtaksdato"))
}
