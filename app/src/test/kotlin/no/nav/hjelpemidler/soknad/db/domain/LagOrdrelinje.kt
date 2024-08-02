package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.behovsmeldingsmodell.ordre.Ordrelinje

fun lagOrdrelinje(søknad: SøknadData): Ordrelinje =
    lagOrdrelinje().copy(søknadId = søknad.soknadId, fnrBruker = søknad.fnrBruker)

fun lagOrdrelinje(): Ordrelinje = Ordrelinje(
    søknadId = lagSøknadId(),
    oebsId = 1000,
    fnrBruker = lagFødselsnummer(),
    serviceforespørsel = 102040,
    ordrenr = 204080,
    ordrelinje = 1,
    delordrelinje = 1,
    artikkelnr = "123456",
    antall = 1.0,
    enhet = "STK",
    produktgruppe = "produktgruppe",
    produktgruppenr = "654321",
    hjelpemiddeltype = "hjelpemiddeltype",
    data = emptyMap(),
)
