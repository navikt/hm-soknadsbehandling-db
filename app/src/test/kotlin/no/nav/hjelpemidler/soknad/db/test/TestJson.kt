package no.nav.hjelpemidler.soknad.db.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hjelpemidler.soknad.db.jsonMapper
import org.intellij.lang.annotations.Language

fun readTree(@Language("JSON") content: String): JsonNode {
    return jsonMapper.readTree(content)
}

fun readMap(@Language("JSON") content: String): Map<String, Any?> {
    return jsonMapper.readValue(content)
}
