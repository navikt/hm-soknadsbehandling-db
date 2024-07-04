package no.nav.hjelpemidler.soknad.db.ordre

import no.nav.hjelpemidler.soknad.db.client.hmdb.HjelpemiddeldatabasenClient
import no.nav.hjelpemidler.soknad.db.client.hmdb.hentproduktermedhmsnrs.Product
import no.nav.hjelpemidler.soknad.db.db.Transaction
import no.nav.hjelpemidler.soknad.db.domain.SøknadForBrukerOrdrelinje
import java.util.UUID

class OrdreService(
    private val transaction: Transaction,
    private val hjelpemiddeldatabasenClient: HjelpemiddeldatabasenClient,
) {
    suspend fun finnOrdreForSøknad(søknadId: UUID): List<SøknadForBrukerOrdrelinje> {
        val ordrelinjer = transaction { ordreStore.ordreForSoknad(søknadId) }
        val produkter = hjelpemiddeldatabasenClient
            .hentProdukterMedHmsnrs(ordrelinjer.map(SøknadForBrukerOrdrelinje::artikkelNr).toSet())
            .groupBy(Product::hmsArtNr)
            .mapValues { it.value.first() }
        return ordrelinjer.map { it.berik(produkter[it.artikkelNr]) }
    }
}
