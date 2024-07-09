package no.nav.hjelpemidler.soknad.db

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonAnySetter
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.soknad.db.test.expect
import no.nav.hjelpemidler.soknad.db.test.testApplication
import java.util.UUID
import kotlin.test.Test

class TokenXRoutesTest {
    @Test
    fun `Hent søknad med søknadId`() = testApplication {
        val søknad = lagreSøknad()

        tokenXUser(søknad.fnrBruker)

        client
            .get("/api/soknad/bruker/${søknad.soknadId}")
            .expect<SøknadDto>(HttpStatusCode.OK) {
                it.søknadId shouldBe søknad.soknadId
            }
    }

    @Test
    fun `Hent søknader for bruker`() = testApplication {
        val søknad = lagreSøknad()

        tokenXUser(søknad.fnrBruker)

        client
            .get("/api/soknad/bruker")
            .expect<List<SøknadDto>>(HttpStatusCode.OK) { søknader ->
                søknader.shouldBeSingleton {
                    it.søknadId shouldBe søknad.soknadId
                }
            }
    }

    @Test
    fun `Hent søknad for innsender`() = testApplication {
        val søknad = lagreSøknad()

        tokenXUser(søknad.fnrInnsender)
        formidlerRolle()

        client
            .get("/api/soknad/innsender/${søknad.soknadId}")
            .expect<SøknadDto>(HttpStatusCode.OK) {
                it.søknadId shouldBe søknad.soknadId
            }
    }
}

private class SøknadDto(
    @JsonAlias("soknadId")
    val søknadId: UUID,
) {
    @JsonAnySetter
    val andre: Map<String, Any?> = mutableMapOf()
}
