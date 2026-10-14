package no.nav.hjelpemidler.soknad.db.ordre

import no.nav.hjelpemidler.soknad.db.domain.SøknadForBrukerOrdrelinje
import no.nav.hjelpemidler.soknad.db.grunndata.GrunndataClient
import no.nav.hjelpemidler.soknad.db.grunndata.hentprodukter.Product
import no.nav.hjelpemidler.soknad.db.store.Transaction
import java.util.UUID

class OrdreService(
    private val transaction: Transaction,
    private val grunndataClient: GrunndataClient,
) {
    suspend fun finnOrdreForSøknad(søknadId: UUID): List<SøknadForBrukerOrdrelinje> {
        val ordrelinjer = transaction { ordreStore.finnOrdreForSøknad(søknadId) }
        val produkter = grunndataClient
            .hentProdukter(ordrelinjer.map(SøknadForBrukerOrdrelinje::artikkelNr).toSet())
            .groupBy(Product::hmsArtNr)
            .mapValues { it.value.first() }
        return ordrelinjer.map { it.berik(produkter[it.artikkelNr]) }
    }
}
