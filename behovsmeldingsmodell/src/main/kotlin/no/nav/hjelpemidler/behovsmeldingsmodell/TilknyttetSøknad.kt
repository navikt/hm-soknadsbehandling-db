package no.nav.hjelpemidler.behovsmeldingsmodell

import com.fasterxml.jackson.annotation.JsonAlias

interface TilknyttetSøknad {
    @get:JsonAlias("soknadId")
    val søknadId: SøknadId
}
