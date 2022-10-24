package no.nav.hjelpemidler.soknad.db

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.Principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import java.net.URL
import java.util.concurrent.TimeUnit

internal fun Application.installAuthentication(
    tokenXConfig: TokenXConfig,
    aadConfig: AadConfig,
    applicationConfig: Configuration.Application
) {

    val jwkProviderTokenX = JwkProviderBuilder(URL(tokenXConfig.metadata.jwksUri))
        // cache up to 10 JWKs for 24 hours
        .cached(10, 24, TimeUnit.HOURS)
        // if not cached, only allow max 10 different keys per minute to be fetched from external provider
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    val jwkProviderAad = JwkProviderBuilder(URL(aadConfig.metadata.jwksUri))
        // cache up to 10 JWKs for 24 hours
        .cached(10, 24, TimeUnit.HOURS)
        // if not cached, only allow max 10 different keys per minute to be fetched from external provider
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt("tokenX") {
            verifier(jwkProviderTokenX, tokenXConfig.metadata.issuer)
            validate { credentials ->
                requireNotNull(credentials.payload.audience) {
                    "Auth: Missing audience in token"
                }
                require(credentials.payload.audience.contains(tokenXConfig.clientId)) {
                    "Auth: Valid audience not found in claims"
                }

                require(credentials.payload.getClaim("acr").asString() == ("Level4")) { "Auth: Level4 required" }
                UserPrincipal(credentials.payload.getClaim(applicationConfig.userclaim).asString())
            }
        }
        jwt("aad") {
            verifier(jwkProviderAad, aadConfig.metadata.issuer)
            validate { credentials ->
                try {
                    requireNotNull(credentials.payload.audience) {
                        "Auth: Missing audience in token"
                    }
                    require(credentials.payload.audience.contains(aadConfig.clientId)) {
                        "Auth: Valid audience not found in claims"
                    }
                    JWTPrincipal(credentials.payload)
                } catch (e: Throwable) {
                    null
                }
            }
        }
    }
}

internal class UserPrincipal(private val fnr: String) : Principal {
    fun getFnr() = fnr
}
