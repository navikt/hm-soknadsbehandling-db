package no.nav.hjelpemidler.soknad.db.store

import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Behovsmelding
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Innsenderbehovsmelding
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.mapping.tilInnsenderbehovsmeldingV2
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.serialization.jackson.jsonToValue

fun tilInnsenderbehovsmelding(json: String): Innsenderbehovsmelding = tilInnsenderbehovsmeldingV2(
    jsonMapper.readValue(json, Behovsmelding::class.java),
)

fun tilInnsenderbehovsmeldingJson(json: String): String = jsonMapper.writeValueAsString(tilInnsenderbehovsmelding(json))

fun tilInnsenderbehovsmeldingMap(json: String): Map<String, Any?> = jsonToValue(tilInnsenderbehovsmeldingJson(json))
