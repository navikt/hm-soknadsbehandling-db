package no.nav.hjelpemidler.soknad.db.soknad

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.soknad.db.test.expect
import no.nav.hjelpemidler.soknad.db.test.testApplication
import java.util.UUID
import kotlin.test.Test

class SøknadApiTest {
    @Test
    fun `Skal hente søknad`() = testApplication {
        val søknadId = lagreSøknad().soknadId
        finnSøknad(søknadId, true).shouldNotBeNull {
            this.søknadId shouldBe søknadId
        }
    }

    @Test
    fun `Skal hente søknad som ikke finnes`() = testApplication {
        val søknadId = UUID.randomUUID()
        val søknad = finnSøknad(søknadId, true)
        søknad shouldBe null
    }

    @Test
    fun `Skal oppdatere journalpostId`() = testApplication {
        val søknadId = lagreSøknad().soknadId
        finnSøknad(søknadId).shouldNotBeNull {
            journalpostId.shouldBeNull()
        }
        val journalpostId = "102030"
        client
            .put(Søknader.SøknadId.Journalpost(søknadId)) {
                setBody(mapOf("journalpostId" to journalpostId))
            }
            .expect(HttpStatusCode.OK, 1)
        finnSøknad(søknadId).shouldNotBeNull {
            journalpostId shouldBe journalpostId
        }
    }

    @Test
    fun `Skal oppdatere oppgaveId`() = testApplication {
        val søknadId = lagreSøknad().soknadId
        finnSøknad(søknadId).shouldNotBeNull {
            oppgaveId.shouldBeNull()
        }
        val oppgaveId = "302010"
        client
            .put(Søknader.SøknadId.Oppgave(søknadId)) {
                setBody(mapOf("oppgaveId" to oppgaveId))
            }
            .expect(HttpStatusCode.OK, 1)
        finnSøknad(søknadId).shouldNotBeNull {
            oppgaveId shouldBe oppgaveId
        }
    }
}
