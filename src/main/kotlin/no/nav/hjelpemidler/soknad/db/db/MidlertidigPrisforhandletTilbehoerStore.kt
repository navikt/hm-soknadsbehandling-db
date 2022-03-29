package no.nav.hjelpemidler.soknad.db.db

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
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

            // Filtrer ut søknader uten noen tilbehør
            if (soknadData.hjelpemidler.hjelpemiddelListe.sumOf { it.tilbehorListe.count() } == 0) return

            // TODO: For hvert tilbehør slå opp rammeavtaleId, og leverandørId
            // TODO: Slå opp om kombinasjonen hmsnrTilbehoer+rammeavtaleId+leverandørId er prisforhandlet
            // TODO: Lagre statistikk i databasen, eventuelt også INFLUX?
            /* using(sessionOf(ds)) { session ->
                session.run(
                    queryOf(
                        "",
                    ).asUpdate
                )
            } */
        } catch (e: Exception) {
            logger.error(e) { "Feil når vi forsøkte å lage statistikk for prisforhandlet tilbehør. Denne kan trygt oversees midlertidig siden den bare påvirker statistikk." }
        }
    }
}
