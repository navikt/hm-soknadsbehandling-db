package no.nav.hjelpemidler.soknad.db

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.call.body
import io.ktor.client.request.get

data class AadConfig(
    val metadata: Metadata,
    val clientId: String,
) {
    data class Metadata(
        @JsonProperty("issuer") val issuer: String,
        @JsonProperty("jwks_uri") val jwksUri: String,
    )
}

suspend fun loadAadConfig(): AadConfig {

    val jwksUri = System.getenv("AZURE_APP_WELL_KNOWN_URL") ?: "http://host.docker.internal:8080/default/.well-known/openid-configuration"
    val clientId = System.getenv("AZURE_APP_CLIENT_ID") ?: "local:hm-soknadsbehandling-db"

    return AadConfig(
        metadata = httpClient().get(jwksUri).body(),
        clientId = clientId
    )
}
