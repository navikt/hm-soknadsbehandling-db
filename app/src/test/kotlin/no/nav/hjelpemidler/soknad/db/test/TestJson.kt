package no.nav.hjelpemidler.soknad.db.test

import com.fasterxml.jackson.databind.JsonNode
import no.nav.hjelpemidler.soknad.db.jsonMapper
import org.intellij.lang.annotations.Language

fun readTree(@Language("JSON") content: String): JsonNode {
    return jsonMapper.readTree(content)
}
