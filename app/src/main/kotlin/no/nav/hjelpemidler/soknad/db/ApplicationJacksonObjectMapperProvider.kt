package no.nav.hjelpemidler.soknad.db

import no.nav.hjelpemidler.serialization.jackson.JacksonObjectMapperProvider
import no.nav.hjelpemidler.serialization.jackson.defaultJsonMapper
import no.nav.hjelpemidler.service.LoadOrder
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.ObjectMapper

@LoadOrder(0)
class ApplicationJacksonObjectMapperProvider : JacksonObjectMapperProvider {
    override fun invoke(): ObjectMapper = defaultJsonMapper {
        disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
    }
}
