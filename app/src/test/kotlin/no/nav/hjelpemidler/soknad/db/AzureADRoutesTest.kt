package no.nav.hjelpemidler.soknad.db

import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldContain
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.Statusendring
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSakId
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSakId
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Sakstilknytning
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Vedtaksresultat
import no.nav.hjelpemidler.soknad.db.domain.HarOrdre
import no.nav.hjelpemidler.soknad.db.domain.UtgåttSøknad
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import no.nav.hjelpemidler.soknad.db.domain.lagOrdrelinje
import no.nav.hjelpemidler.soknad.db.domain.lagPapirsøknad
import no.nav.hjelpemidler.soknad.db.domain.lagSøknadId
import no.nav.hjelpemidler.soknad.db.test.expect
import no.nav.hjelpemidler.soknad.db.test.testApplication
import java.time.LocalDate
import kotlin.test.Test

class AzureADRoutesTest {
    @Test
    fun `Skal lagre ordre`() = testApplication {
        val søknad = lagreSøknad()
        client
            .post("/api/ordre") { setBody(lagOrdrelinje(søknad)) }
            .expect(HttpStatusCode.Created, 1)
    }

    @Test
    fun `Skal lagre papirsøknad`() = testApplication {
        client
            .post("/api/soknad/papir") { setBody(lagPapirsøknad()) }
            .expect(HttpStatusCode.Created, 1)
    }

    @Test
    fun `Skal lagre vedtaksresultat fra Infotrygd`() = testApplication {
        val søknadId = lagSøknadId()
        val sakstilknytning = Sakstilknytning.Infotrygd(InfotrygdSakId("9999A01"), lagFødselsnummer())
        val vedtaksresultat = Vedtaksresultat.Infotrygd("I", LocalDate.now(), "HJDAANS")

        lagreSakstilknytning(søknadId, sakstilknytning)
        lagreVedtaksresultat(søknadId, vedtaksresultat)

        client
            .post("/api/soknad/fra-vedtaksresultat-v2") {
                setBody(
                    mapOf(
                        "fnrBruker" to sakstilknytning.fnrBruker,
                        "saksblokkOgSaksnr" to sakstilknytning.sakId.toString().takeLast(3),
                    ),
                )
            }
            .expect<List<Map<String, Any?>>>(HttpStatusCode.OK) { resultater ->
                resultater.shouldBeSingleton {
                    it.shouldContain("søknadId", søknadId.toString())
                }
            }
    }

    @Test
    fun `Skal lagre knytning mellom søknad og sak fra Hotsak`() = testApplication {
        val søknad = lagreSøknad()
        val søknadId = søknad.soknadId
        lagreSakstilknytning(søknadId, Sakstilknytning.Hotsak(HotsakSakId("1020")))
        lagreVedtaksresultat(søknadId, Vedtaksresultat.Hotsak("I", LocalDate.now()))
    }

    @Test
    fun `Skal slette søknad`() = testApplication {
        val søknad = lagreSøknad()
        val søknadId = søknad.soknadId
        client
            .delete("/api/soknad/bruker") { setBody(søknadId) }
            .expect(HttpStatusCode.OK, 1)
    }

    @Test
    fun `Skal slette utløpt søknad`() = testApplication {
        val søknad = lagreSøknad()
        val søknadId = søknad.soknadId
        client
            .delete("/api/soknad/utlopt/bruker") { setBody(søknadId) }
            .expect(HttpStatusCode.OK, 1)
    }

    @Test
    fun `Skal oppdatere status på søknad`() = testApplication {
        val søknad = lagreSøknad()
        val søknadId = søknad.soknadId
        client
            .put("/api/soknad/$søknadId/status") {
                setBody(
                    Statusendring(
                        status = BehovsmeldingStatus.ENDELIG_JOURNALFØRT,
                        valgteÅrsaker = setOf("årsak"),
                        begrunnelse = "begrunnelse",
                    ),
                )
            }
            .expect(HttpStatusCode.OK, 1)
    }

    @Test
    fun `Skal sjekke om fnr og journalpostId finnes`() = testApplication {
        val søknad = lagreSøknad()
        val journalpostId = 102030
        val dto = mapOf("fnrBruker" to søknad.fnrBruker, "journalpostId" to journalpostId)
        client
            .post("/api/infotrygd/fnr-jounralpost") { setBody(dto) }
            .expect(HttpStatusCode.OK, "fnrOgJournalpostIdFinnes" to false)
        oppdaterJournalpostId(søknad.soknadId, journalpostId.toString())
        oppdaterOppgaveId(søknad.soknadId, "302010")
        client
            .post("/api/infotrygd/fnr-jounralpost") { setBody(dto) }
            .expect(HttpStatusCode.OK, "fnrOgJournalpostIdFinnes" to true)
    }

    @Test
    fun `Skal hente søknader til godkjenning eldre enn 0 dager`() = testApplication {
        lagreSøknad()
        client.get("/api/soknad/utgaatt/0")
            .expect<List<UtgåttSøknad>>(HttpStatusCode.OK) {
                it.shouldNotBeEmpty()
            }
    }

    @Test
    fun `Skal sjekke om ordre har blitt oppdatert siste døgn`() = testApplication {
        val søknad = lagreSøknad()
        client.get("/api/soknad/ordre/ordrelinje-siste-doegn/${søknad.soknadId}")
            .expect(HttpStatusCode.OK, HarOrdre(harOrdreAvTypeHjelpemidler = false, harOrdreAvTypeDel = false))
    }

    @Test
    fun `Skal sjekke om søknad har ordre`() = testApplication {
        val søknad = lagreSøknad()
        client.get("/api/soknad/ordre/har-ordre/${søknad.soknadId}")
            .expect(HttpStatusCode.OK, HarOrdre(harOrdreAvTypeHjelpemidler = false, harOrdreAvTypeDel = false))
    }

    @Test
    fun `Skal hente datasett for forslagsmotoren`() = testApplication {
        client.get("/api/forslagsmotor/tilbehoer/datasett")
            .expect<Any?>(HttpStatusCode.OK)
    }
}
