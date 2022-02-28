package no.nav.hjelpemidler.soknad.db

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.request.get

data class TokenXConfig(
    val metadata: Metadata,
    val clientId: String,
) {
    data class Metadata(
        @JsonProperty("issuer") val issuer: String,
        @JsonProperty("jwks_uri") val jwksUri: String,
    )
}

suspend fun loadTokenXConfig(): TokenXConfig {

    val jwksUri = System.getenv("TOKEN_X_WELL_KNOWN_URL") ?: "http://host.docker.internal:8080/default/.well-known/openid-configuration"
    val clientId = System.getenv("TOKEN_X_CLIENT_ID") ?: "local"

    return TokenXConfig(
        metadata = httpClient().get(jwksUri),
        clientId = clientId
    )
}
