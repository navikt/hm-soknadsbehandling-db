package no.nav.hjelpemidler.soknad.db

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.request.get
import io.ktor.util.KtorExperimentalAPI

data class AadConfig(
    val metadata: Metadata,
    val clientId: String,
) {
    data class Metadata(
        @JsonProperty("issuer") val issuer: String,
        @JsonProperty("jwks_uri") val jwksUri: String,
    )
}

@KtorExperimentalAPI
suspend fun loadAadConfig(): AadConfig {

    val jwksUri = System.getenv("TOKEN_X_WELL_KNOWN_URL") ?: "http://host.docker.internal:8080/default/.well-known/openid-configuration"
    val clientId = System.getenv("TOKEN_X_CLIENT_ID") ?: "local:hm-soknadsbehandling-db"

    return AadConfig(
        metadata = httpClient().get(jwksUri),
        clientId = clientId
    )
}
