package no.nav.hjelpemidler.soknad.db.db

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.hjelpemidler.soknad.db.client.hmdb.HjelpemiddeldatabaseClient
import no.nav.hjelpemidler.soknad.db.domain.SoknadData
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

internal interface MidlertidigPrisforhandletTilbehoerStore {
    fun lagStatistikkForPrisforhandletTilbehoer(soknad: SoknadData)
}

internal class MidlertidigPrisforhandletTilbehoerStorePostgres(private val ds: DataSource) : MidlertidigPrisforhandletTilbehoerStore {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    override fun lagStatistikkForPrisforhandletTilbehoer(soknad: SoknadData) {
        try {
            // Hent ut nødvendig info fra søknad data
            data class Tilbehoer(
                val hmsnr: String,
            )

            data class Hjelpemiddel(
                val hmsNr: String,
                val tilbehorListe: List<Tilbehoer>,
            )

            data class HjelpemiddelListe(
                val hjelpemiddelListe: List<Hjelpemiddel>,
            )

            data class Hjelpemidler(
                val hjelpemidler: HjelpemiddelListe,
            )

            val soknadData: Hjelpemidler = objectMapper.readValue(objectMapper.writeValueAsString(soknad.soknad))

            // Filtrer ut søknader uten noen tilbehør, kna data, og loop over det som er igjen
            val hjelpemidler = soknadData.hjelpemidler.hjelpemiddelListe
                .filter { it.tilbehorListe.isNotEmpty() }
                .groupBy { it.hmsNr }
                .mapValues {
                    it.value
                        .map { it.tilbehorListe }
                        // Merge lists
                        .fold(mutableListOf<Tilbehoer>()) { a, b ->
                            a.addAll(b)
                            a
                        }
                        // Transform into distinct list of hmsnrs
                        .groupBy { it.hmsnr }
                        .map { it.key }
                }

            val precachedRammeavtaleLeverandor = runBlocking {
                HjelpemiddeldatabaseClient.hentRammeavtaleIdOgLeverandorIdForProdukter(hjelpemidler.keys.toSet())
            }
                .groupBy { it.hmsnr!! }
                .filter { it.value.isNotEmpty() }
                .mapValues { it.value.first() }

            hjelpemidler.forEach { hjelpemiddel ->
                    val hmsnr_hjelpemiddel = hjelpemiddel.key
                    val tilbehoer = hjelpemiddel.value

                    // Slå opp rammeavtaleId og leverandørId for hjelpemiddel
                    val rammeavtaleLeverandor = precachedRammeavtaleLeverandor[hmsnr_hjelpemiddel] ?: return

                    // Forbered listen over prisforhandligner vi skal slå opp
                    data class Prisforhandling(
                        val hmsnr_tilbehoer: String,
                        val rammeavtaleId: String,
                        val leverandorId: String,
                    )

                    val prisforhandlinger = tilbehoer.map { prisforhandling ->
                        Prisforhandling(
                            hmsnr_tilbehoer = prisforhandling,
                            rammeavtaleId = rammeavtaleLeverandor.rammeavtaleId!!,
                            leverandorId = rammeavtaleLeverandor.leverandorId!!,
                        )
                    }

                    // Slå opp om kombinasjonen hmsnrTilbehoer+rammeavtaleId+leverandørId er prisforhandlet
                    val prisforhandlet = prisforhandlinger.map { it ->
                        Pair(it, runBlocking { HjelpemiddeldatabaseClient.erPrisforhandletTilbehoer(it.hmsnr_tilbehoer, it.rammeavtaleId, it.leverandorId) })
                    }

                    // TODO: Lagre statistikk i databasen, eventuelt også INFLUX?
                    prisforhandlet.forEach {
                        using(sessionOf(ds)) { session ->
                            session.run(
                                queryOf(
                                    """
                                        INSERT INTO v1_midlertidig_prisforhandlet_tilbehoer (
                                            hmsnr_hjelpemiddel,
                                            hmsnr_tilbehoer,
                                            rammeavtale_id,
                                            leverandor_id,
                                            prisforhandlet,
                                            soknads_id
                                        ) VALUES (?, ?, ?, ?, ?, ?);
                                    """.trimIndent(),
                                    hmsnr_hjelpemiddel,
                                    it.first.hmsnr_tilbehoer,
                                    it.first.rammeavtaleId,
                                    it.first.leverandorId,
                                    it.second,
                                    soknad.soknadId,
                                ).asUpdate
                            )
                        }
                    }
                }

            // TODO: Add simple way of extracting the stats
        } catch (e: Exception) {
            logger.error(e) { "Feil når vi forsøkte å lage statistikk for prisforhandlet tilbehør. Denne kan trygt oversees midlertidig siden den bare påvirker statistikk." }
        }
    }
}
