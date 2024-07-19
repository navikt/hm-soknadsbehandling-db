package no.nav.hjelpemidler.soknad.db.soknad

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.soknad.db.test.expect
import no.nav.hjelpemidler.soknad.db.test.testApplication
import kotlin.test.Test

class SøknadApiTest {
    @Test
    fun `Skal hente søknad`() = testApplication {
        val søknadId = lagreSøknad().soknadId
        val søknad = hentSøknad(søknadId)
        søknad.søknadId shouldBe søknadId
    }

    @Test
    fun `Skal oppdatere journalpostId`() = testApplication {
        val søknadId = lagreSøknad().soknadId
        hentSøknad(søknadId).should {
            it.journalpostId.shouldBeNull()
        }
        val journalpostId = "102030"
        client
            .put(Søknader.SøknadId.Journalpost(søknadId)) {
                setBody(mapOf("journalpostId" to journalpostId))
            }
            .expect(HttpStatusCode.OK, 1)
        hentSøknad(søknadId).should {
            it.journalpostId shouldBe journalpostId
        }
    }

    @Test
    fun `Skal oppdatere oppgaveId`() = testApplication {
        val søknadId = lagreSøknad().soknadId
        hentSøknad(søknadId).should {
            it.oppgaveId.shouldBeNull()
        }
        val oppgaveId = "302010"
        client
            .put(Søknader.SøknadId.Oppgave(søknadId)) {
                setBody(mapOf("oppgaveId" to oppgaveId))
            }
            .expect(HttpStatusCode.OK, 1)
        hentSøknad(søknadId).should {
            it.oppgaveId shouldBe oppgaveId
        }
    }
}
