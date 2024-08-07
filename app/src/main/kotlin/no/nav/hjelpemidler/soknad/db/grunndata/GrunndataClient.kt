package no.nav.hjelpemidler.soknad.db.grunndata

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import no.nav.hjelpemidler.soknad.db.Configuration
import no.nav.hjelpemidler.soknad.db.client.hmdb.HentProdukterMedHmsnrs
import java.net.URI
import no.nav.hjelpemidler.soknad.db.client.hmdb.hentproduktermedhmsnrs.Product as HentProdukterMedHmsnrsProdukt

private val logg = KotlinLogging.logger {}

class GrunndataClient {
    private val client = GraphQLKtorClient(
        url = URI(Configuration.GRUNNDATA_API_URL).toURL(),
        httpClient = HttpClient(engineFactory = Apache),
        serializer = GraphQLClientJacksonSerializer(),
    )

    suspend fun hentProdukterMedHmsnrs(hmsnrs: Set<String>): List<HentProdukterMedHmsnrsProdukt> {
        if (hmsnrs.isEmpty()) return emptyList()
        logg.debug { "Henter produkter med hmsnrs: $hmsnrs fra hjelpemiddeldatabasen" }
        val request = HentProdukterMedHmsnrs(variables = HentProdukterMedHmsnrs.Variables(hmsnrs = hmsnrs.toList()))
        return try {
            val response = client.execute(request)
            when {
                response.errors != null -> {
                    logg.error { "Feil under henting av data fra hjelpemiddeldatabasen, hmsnrs: $hmsnrs, errors: ${response.errors?.map { it.message }}" }
                    emptyList()
                }

                response.data != null -> {
                    val produkter = response.data?.produkter ?: emptyList()
                    logg.debug { "Hentet ${produkter.size} produkter fra hjelpemiddeldatabasen" }
                    produkter
                }

                else -> emptyList()
            }
        } catch (e: Exception) {
            logg.error(e) { "Feil under henting av data fra hjelpemiddeldatabasen, hmsnrs: $hmsnrs" }
            return emptyList()
        }
    }
}
