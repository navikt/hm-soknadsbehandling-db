package no.nav.hjelpemidler.soknad.db.grunndata

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.defaultRequest
import no.nav.hjelpemidler.http.correlationId
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.soknad.db.grunndata.hentprodukter.Product
import no.nav.hjelpemidler.text.toURL
import tools.jackson.databind.json.JsonMapper

private val log = KotlinLogging.logger {}

class GrunndataClient(
    engine: HttpClientEngine,
    url: String,
) {
    private val client = GraphQLKtorClient(
        url = url.toURL(),
        httpClient = HttpClient(engine) {
            defaultRequest { correlationId() }
        },
        serializer = GraphQLClientJacksonSerializer(jsonMapper as JsonMapper),
    )

    suspend fun hentProdukter(hmsnrs: Set<String>): List<Product> {
        if (hmsnrs.isEmpty()) return emptyList()
        log.debug { "Henter produkter fra grunndata, hmsnrs: $hmsnrs" }
        val request = HentProdukter(variables = HentProdukter.Variables(hmsnrs = hmsnrs.toList()))
        return try {
            val response = client.execute(request)
            when {
                response.errors != null -> {
                    log.error { "Feil under henting av produkter fra grunndata, hmsnrs: $hmsnrs, errors: ${response.errors?.map { it.message }}" }
                    emptyList()
                }

                response.data != null -> {
                    val produkter = response.data?.produkter ?: emptyList()
                    log.debug { "Hentet ${produkter.size} produkter fra grunndata" }
                    produkter
                }

                else -> emptyList()
            }
        } catch (e: Exception) {
            log.error(e) { "Feil under henting av data fra grunndata, hmsnrs: $hmsnrs" }
            return emptyList()
        }
    }
}
