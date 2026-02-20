package no.nav.hjelpemidler.soknad.db.store

import no.nav.hjelpemidler.soknad.db.domain.kommuneapi.v2.Fødselsnummer

data class InfotrygdDigitaltVedtakNøkkel(
    val fnr: Fødselsnummer,
    val trygdekontornummer: String,
    val saksblokk: String,
    val saksnummer: String,
)
