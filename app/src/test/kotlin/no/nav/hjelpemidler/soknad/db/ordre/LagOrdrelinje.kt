package no.nav.hjelpemidler.soknad.db.ordre

import no.nav.hjelpemidler.behovsmeldingsmodell.ordre.Ordrelinje
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import no.nav.hjelpemidler.soknad.db.soknad.lagSøknadId

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
