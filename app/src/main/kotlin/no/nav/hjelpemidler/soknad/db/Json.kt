package no.nav.hjelpemidler.soknad.db

import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import no.nav.hjelpemidler.configuration.Environment

val jsonMapper: JsonMapper = jacksonMapperBuilder()
    .addModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION, !Environment.current.tier.isProd)
    .build()

class DatabaseJsonMapper : no.nav.hjelpemidler.database.JsonMapper {
    override fun <T> convertValue(fromValue: Any?, toValueTypeRef: TypeReference<T>): T {
        return jsonMapper.convertValue(fromValue, toValueTypeRef)
    }

    override fun <T> readValue(content: String?, valueTypeRef: TypeReference<T>): T {
        return jsonMapper.readValue(content, valueTypeRef)
    }

    override fun <T> readValue(src: ByteArray?, valueTypeRef: TypeReference<T>): T {
        return jsonMapper.readValue(src, valueTypeRef)
    }

    override fun <T> writeValueAsString(value: T): String {
        return jsonMapper.writeValueAsString(value)
    }
}
