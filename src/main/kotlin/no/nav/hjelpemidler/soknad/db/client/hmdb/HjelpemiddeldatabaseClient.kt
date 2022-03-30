package no.nav.hjelpemidler.soknad.db.client.hmdb

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import mu.KotlinLogging
import no.nav.hjelpemidler.soknad.db.Configuration
import java.net.URL
import no.nav.hjelpemidler.soknad.db.client.hmdb.hentproduktermedhmsnrs.Produkt as HentproduktermedhmsnrsProdukt
import no.nav.hjelpemidler.soknad.db.client.hmdb.hentrammeavtaleidogleverandoridforprodukter.Produkt as HentrammeavtaleidogleverandoridforprodukterProdukt

object HjelpemiddeldatabaseClient {
    private val logg = KotlinLogging.logger {}
    private val client =
        GraphQLKtorClient(
            url = URL("${Configuration.application.grunndataApiURL}/graphql"),
            httpClient = HttpClient(engineFactory = Apache),
            serializer = GraphQLClientJacksonSerializer()
        )

    suspend fun hentProdukterMedHmsnrs(hmsnrs: Set<String>): List<HentproduktermedhmsnrsProdukt> {
        if (hmsnrs.isEmpty()) return emptyList()
        logg.debug { "Henter produkter med hmsnrs=$hmsnrs fra hjelpemiddeldatabasen" }
        val request = HentProdukterMedHmsnrs(variables = HentProdukterMedHmsnrs.Variables(hmsnrs = hmsnrs.toList()))
        return try {
            val response = client.execute(request)
            when {
                response.errors != null -> {
                    logg.error { "Feil under henting av data fra hjelpemiddeldatabasen, hmsnrs=$hmsnrs, errors=${response.errors?.map { it.message }}" }
                    emptyList()
                    // fixme -> feil hardt i dev?
                }
                response.data != null -> {
                    val produkter = response.data?.produkter ?: emptyList()
                    logg.debug { "Hentet ${produkter.size} produkter fra hjelpemiddeldatabasen" }
                    produkter
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            logg.error(e) { "Feil under henting av data fra hjelpemiddeldatabasen, hmsnrs=$hmsnrs" }
            return emptyList()
        }
    }

    suspend fun hentRammeavtaleIdOgLeverandorIdForProdukter(hmsnrs: Set<String>): List<HentrammeavtaleidogleverandoridforprodukterProdukt> {
        if (hmsnrs.isEmpty()) return emptyList()
        logg.debug { "Henter rammeavtaleid og leverandorid for produkter med hmsnrs=$hmsnrs fra hjelpemiddeldatabasen" }
        val request = HentRammeavtaleIdOgLeverandorIdForProdukter(variables = HentRammeavtaleIdOgLeverandorIdForProdukter.Variables(hmsnrs = hmsnrs.toList()))
        return try {
            val response = client.execute(request)
            when {
                response.errors != null -> {
                    logg.error { "Feil under henting av data fra hjelpemiddeldatabasen, hmsnrs=$hmsnrs, errors=${response.errors?.map { it.message }}" }
                    emptyList()
                    // fixme -> feil hardt i dev?
                }
                response.data != null -> {
                    val produkter = response.data?.produkter ?: emptyList()
                    logg.debug { "Hentet ${produkter.size} rammeavtaleid/leverandorid (produkter) fra hjelpemiddeldatabasen" }
                    produkter
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            logg.error(e) { "Feil under henting av data fra hjelpemiddeldatabasen, hmsnrs=$hmsnrs" }
            return emptyList()
        }
    }

    suspend fun erPrisforhandletTilbehoer(hmsnr: String, rammeavtaleId: String, leverandorId: String): Boolean {
        logg.debug { "Henter erPrisforhandletTilbehoer (for hmsnrs=$hmsnr, rammeavtaleId=$rammeavtaleId, leverandorId=$leverandorId) fra hjelpemiddeldatabasen" }
        val request = ErPrisforhandletTilbehoer(
            variables = ErPrisforhandletTilbehoer.Variables(
                hmsnr = hmsnr,
                rammeavtaleId = rammeavtaleId,
                leverandorId = leverandorId,
            )
        )
        return try {
            val response = client.execute(request)
            when {
                response.errors != null -> {
                    logg.error { "Feil under henting av data fra hjelpemiddeldatabasen (hmsnr=$hmsnr, rammeavtaleId=$rammeavtaleId, leverandorId=$leverandorId), errors=${response.errors?.map { it.message }}" }
                    false
                    // fixme -> feil hardt i dev?
                }
                response.data != null -> {
                    logg.debug { "Hentet erPrisforhandletTilbehoer fra hjelpemiddeldatabasen" }
                    response.data?.prisforhandlet ?: false
                }
                else -> false
            }
        } catch (e: Exception) {
            logg.error(e) { "Feil under henting av data fra hjelpemiddeldatabasen: hmsnr=$hmsnr, rammeavtaleId=$rammeavtaleId, leverandorId=$leverandorId" }
            return false
        }
    }
}
