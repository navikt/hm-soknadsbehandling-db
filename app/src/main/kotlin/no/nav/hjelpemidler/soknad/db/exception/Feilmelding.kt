package no.nav.hjelpemidler.soknad.db.exception

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.ser.std.StdSerializer
import java.time.Instant

/**
 * [Inspirert av Spring Boot](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/web/servlet/error/DefaultErrorAttributes.html)
 */
data class Feilmelding(
    val timestamp: Instant = Instant.now(),
    @JsonSerialize(using = HttpStatusCodeSerializer::class)
    @JsonDeserialize(using = HttpStatusCodeDeserializer::class)
    val status: HttpStatusCode,
    val error: String,
    val message: String,
    val trace: String? = null,
    val path: String,
) {
    constructor(
        call: ApplicationCall,
        status: HttpStatusCode,
        message: String? = null,
        trace: String? = null,
    ) : this(
        status = status,
        error = status.description,
        message = message ?: status.toString(),
        trace = trace,
        path = call.request.path(),
    )

    constructor(
        call: ApplicationCall,
        cause: Throwable,
        status: HttpStatusCode,
        message: String? = null,
        trace: String? = null,
    ) : this(
        status = status,
        error = status.description,
        message = message ?: cause.message ?: status.toString(),
        trace = trace ?: cause.trace,
        path = call.request.path(),
    )
}

private class HttpStatusCodeSerializer : StdSerializer<HttpStatusCode>(HttpStatusCode::class.java) {
    override fun serialize(value: HttpStatusCode, generator: JsonGenerator, context: SerializationContext) {
        generator.writeNumber(value.value)
    }
}

private class HttpStatusCodeDeserializer : StdDeserializer<HttpStatusCode>(HttpStatusCode::class.java) {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): HttpStatusCode = HttpStatusCode.fromValue(parser.intValue)
}
