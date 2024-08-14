package no.nav.hjelpemidler.soknad.db

import io.kotest.matchers.collections.shouldHaveSingleElement
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.soknad.db.domain.kommuneapi.SøknadForKommuneApi
import no.nav.hjelpemidler.soknad.db.test.expect
import no.nav.hjelpemidler.soknad.db.test.feilmelding
import no.nav.hjelpemidler.soknad.db.test.testApplication
import kotlin.test.Test

class KommuneApiTest {
    @Test
    fun `Skal hente søknader`() = testApplication {
        val grunnlag = lagreBehovsmelding()
        client
            .post(KommuneApi.Søknader()) { setBody(mapOf("kommunenummer" to "9999")) }
            .expect<List<SøknadForKommuneApi>>(HttpStatusCode.OK) { søknader ->
                søknader.shouldHaveSingleElement { it.soknadId == grunnlag.søknadId }
            }
    }

    @Test
    fun `Henting av søknader feiler`() = testApplication {
        lagreBehovsmelding()
        client
            .post(KommuneApi.Søknader()) { setBody(mapOf("kommunenummer" to "-1")) }
            .feilmelding(HttpStatusCode.BadRequest)
        client
            .post(KommuneApi.Søknader()) { setBody(mapOf("foobar" to "9999")) }
            .feilmelding(HttpStatusCode.BadRequest)
    }
}
