package no.nav.hjelpemidler.soknad.db

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonAnySetter
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.resources.get
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.soknad.db.resources.Søknader
import no.nav.hjelpemidler.soknad.db.test.expect
import no.nav.hjelpemidler.soknad.db.test.testApplication
import java.util.UUID
import kotlin.test.Test

class TokenXRoutesTest {
    @Test
    fun `Hent søknad med søknadId for bruker`() = testApplication {
        val søknad = lagreSøknad()

        tokenXUser(søknad.fnrBruker)

        client
            .get(Søknader.Bruker.SøknadId(søknad.soknadId))
            .expect<SøknadDto>(HttpStatusCode.OK) {
                it.søknadId shouldBe søknad.soknadId
            }
    }

    @Test
    fun `Hent søknader for bruker`() = testApplication {
        val søknad = lagreSøknad()

        tokenXUser(søknad.fnrBruker)

        client
            .get(Søknader.Bruker())
            .expect<List<SøknadDto>>(HttpStatusCode.OK) { søknader ->
                søknader.shouldBeSingleton {
                    it.søknadId shouldBe søknad.soknadId
                }
            }
    }

    @Test
    fun `Hent søknad med søknadId for innsender`() = testApplication {
        val søknad = lagreSøknad()

        tokenXUser(søknad.fnrInnsender)
        formidlerRolle()

        client
            .get(Søknader.Innsender.SøknadId(søknad.soknadId))
            .expect<SøknadDto>(HttpStatusCode.OK) {
                it.søknadId shouldBe søknad.soknadId
            }
    }

    @Test
    fun `Hent søknader for innsender`() = testApplication {
        val søknad = lagreSøknad()

        tokenXUser(søknad.fnrInnsender)
        formidlerRolle()

        client
            .get(Søknader.Innsender())
            .expect<List<SøknadDto>>(HttpStatusCode.OK) { søknader ->
                søknader.shouldBeSingleton {
                    it.søknadId shouldBe søknad.soknadId
                }
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
