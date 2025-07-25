package no.nav.hjelpemidler.soknad.db

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.soknad.db.domain.HarOrdre
import no.nav.hjelpemidler.soknad.db.domain.UtgåttSøknad
import no.nav.hjelpemidler.soknad.db.test.expect
import no.nav.hjelpemidler.soknad.db.test.testApplication
import kotlin.test.Test

class AzureADRoutesTest {
    @Test
    fun `Skal hente søknader til godkjenning eldre enn 0 dager`() = testApplication {
        lagreBehovsmelding()
        client.get("/api/soknad/utgaatt/0")
            .expect<List<UtgåttSøknad>>(HttpStatusCode.OK) {
                it.shouldNotBeEmpty()
            }
    }

    @Test
    fun `Skal sjekke om ordre har blitt oppdatert siste døgn`() = testApplication {
        val grunnlag = lagreBehovsmelding()
        client.get("/api/soknad/ordre/ordrelinje-siste-doegn/${grunnlag.søknadId}")
            .expect(HttpStatusCode.OK, HarOrdre(harOrdreAvTypeHjelpemidler = false, harOrdreAvTypeDel = false))
    }

    @Test
    fun `Skal sjekke om søknad har ordre`() = testApplication {
        val grunnlag = lagreBehovsmelding()
        client.get("/api/soknad/ordre/har-ordre/${grunnlag.søknadId}")
            .expect(HttpStatusCode.OK, HarOrdre(harOrdreAvTypeHjelpemidler = false, harOrdreAvTypeDel = false))
    }
}
