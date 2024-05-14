package no.nav.hjelpemidler.behovsmeldingsmodell.test

import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.assertNotNull

val jsonMapper: JsonMapper = jacksonMapperBuilder()
    .addModule(JavaTimeModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
    .build()

inline fun <reified T : Any> JsonMapper.readResource(name: String): T =
    assertNotNull(javaClass.getResourceAsStream(name), "$name var null").use {
        jsonMapper.readValue<T>(it)
    }
