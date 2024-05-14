package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.kotest.matchers.collections.shouldExist
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingResponse
import no.nav.hjelpemidler.soknad.db.jsonMapper
import kotlin.test.Test

class SoknadDataTest {
    @Test
    fun `SoknadData skal kunne deserialiseres til BehovsmeldingResponse`() {
        val properties1 = jsonMapper.introspect<SoknadData>().findProperties()
        val properties2 = jsonMapper.introspect<BehovsmeldingResponse>().findProperties()

        properties1.forEach { property1 ->
            properties2 shouldExist { property2 ->
                property2.name == property1.name || property1.fullName in property2.findAliases()
            }
        }
    }
}

private inline fun <reified T> JsonMapper.introspect(): BeanDescription {
    val type = constructType(jacksonTypeRef<T>())
    return deserializationConfig.introspect(type)
}
