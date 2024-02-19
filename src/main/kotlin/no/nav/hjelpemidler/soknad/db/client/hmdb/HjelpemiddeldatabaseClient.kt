package no.nav.hjelpemidler.soknad.db.client.hmdb

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import mu.KotlinLogging
import no.nav.hjelpemidler.soknad.db.Configuration
import no.nav.hjelpemidler.soknad.db.client.`hmdb-ng`.enums.MediaType
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

                    // TODO: Remove when old grunndata-api is replaced in prod., and old hmdb is hmdb-ng
                    runCatching {
                        val produkterAlt = runCatching {
                            no.nav.hjelpemidler.soknad.db.client.`hmdb-ng`.HjelpemiddeldatabaseNgClient.hentProdukterMedHmsnrs(hmsnrs)
                        }.getOrElse { e ->
                            logg.error(e) { "DEBUG GRUNNDATA: Exception while fetching hmdb-ng: $e" }
                            listOf()
                        }

                        val missingHmsnrs: MutableList<String> = mutableListOf()
                        val unexpectedDataHmsnrs: MutableMap<String, Pair<String, String>> = mutableMapOf()
                        val matchesHmsnrs: MutableMap<String, Pair<String, String>> = mutableMapOf()
                        produkter.forEach { old ->
                            if (old.hmsnr == null) return@forEach
                            val new = produkterAlt.find { it.hmsArtNr == old.hmsnr }
                            if (new == null) {
                                missingHmsnrs.add(old.hmsnr)
                            } else if (
                                old.artikkelId != new.identifier.removePrefix("HMDB-") ||
                                old.artikkelnavn != new.articleName ||
                                old.produktId != new.seriesId?.removePrefix("HMDB-") ||
                                old.produktbeskrivelse != new.attributes.text ||
                                old.isotittel != new.isoCategoryTitle ||
                                old.blobUrlLite != null &&
                                new.media.find { it.type == MediaType.IMAGE && it.priority == 1 } == null
                            ) {
                                unexpectedDataHmsnrs[old.hmsnr] = Pair(old.toString(), new.toString())
                            } else {
                                matchesHmsnrs[old.hmsnr] = Pair(old.toString(), new.toString())
                            }
                        }
                        if (missingHmsnrs.isNotEmpty()) {
                            logg.info("DEBUG GRUNNDATA: new dataset missing results for hmsnrs=$missingHmsnrs")
                        }
                        if (unexpectedDataHmsnrs.isNotEmpty()) {
                            logg.info("DEBUG GRUNNDATA: new dataset has mismatching data: $unexpectedDataHmsnrs")
                        }
                        if (matchesHmsnrs.isNotEmpty()) {
                            logg.info("DEBUG GRUNNDATA: new dataset matches old for hmsnrs/data: $matchesHmsnrs")
                        }
                    }.getOrNull()

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

                    // TODO: Remove when old grunndata-api is replaced in prod., and old hmdb is hmdb-ng
                    runCatching {
                        val produkterAlt = runCatching {
                            no.nav.hjelpemidler.soknad.db.client.`hmdb-ng`.HjelpemiddeldatabaseNgClient.hentRammeavtaleIdOgLeverandorIdForProdukter(hmsnrs)
                        }.getOrElse { e ->
                            logg.error(e) { "DEBUG GRUNNDATA 2: Exception while fetching hmdb-ng: $e" }
                            listOf()
                        }

                        val missingHmsnrs: MutableList<String> = mutableListOf()
                        val unexpectedDataHmsnrs: MutableMap<String, Pair<String, String>> = mutableMapOf()
                        val matchesHmsnrs: MutableMap<String, Pair<String, String>> = mutableMapOf()
                        produkter.forEach { old ->
                            if (old.hmsnr == null) return@forEach
                            val new = produkterAlt.find { it.hmsArtNr == old.hmsnr }
                            if (new == null) {
                                missingHmsnrs.add(old.hmsnr)
                            } else if (
                                old.rammeavtaleId != null && (
                                    new.agreements.find { it.identifier?.endsWith(old.rammeavtaleId) == true } == null
                                    ) ||
                                old.leverandorId != null && (
                                    old.leverandorId != new.supplier.identifier.removePrefix("HMDB-")
                                    )
                            ) {
                                unexpectedDataHmsnrs[old.hmsnr] = Pair(old.toString(), new.toString())
                            } else {
                                matchesHmsnrs[old.hmsnr] = Pair(old.toString(), new.toString())
                            }
                        }
                        if (missingHmsnrs.isNotEmpty()) {
                            logg.info("DEBUG GRUNNDATA 2: new dataset missing results for hmsnrs=$missingHmsnrs")
                        }
                        if (unexpectedDataHmsnrs.isNotEmpty()) {
                            logg.info("DEBUG GRUNNDATA 2: new dataset has mismatching data: $unexpectedDataHmsnrs")
                        }
                        if (matchesHmsnrs.isNotEmpty()) {
                            logg.info("DEBUG GRUNNDATA 2: new dataset matches old for hmsnrs/data: $matchesHmsnrs")
                        }
                    }.getOrNull()

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
