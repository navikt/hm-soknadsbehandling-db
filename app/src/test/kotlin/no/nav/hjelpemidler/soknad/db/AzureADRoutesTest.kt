package no.nav.hjelpemidler.soknad.db

import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.soknad.db.domain.HarOrdre
import no.nav.hjelpemidler.soknad.db.domain.HotsakTilknytningData
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.domain.StatusMedÅrsak
import no.nav.hjelpemidler.soknad.db.domain.UtgåttSøknad
import no.nav.hjelpemidler.soknad.db.domain.kommuneapi.SøknadForKommuneApi
import no.nav.hjelpemidler.soknad.db.domain.lagOrdrelinje
import no.nav.hjelpemidler.soknad.db.domain.lagPapirsøknad
import no.nav.hjelpemidler.soknad.db.domain.lagSøknadId
import no.nav.hjelpemidler.soknad.db.domain.lagVedtaksresultat1
import no.nav.hjelpemidler.soknad.db.domain.lagVedtaksresultat2
import no.nav.hjelpemidler.soknad.db.test.expect
import no.nav.hjelpemidler.soknad.db.test.testApplication
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.test.Test

class AzureADRoutesTest {
    @Test
    fun `Skal hente fnr for søknad`() = testApplication {
        val søknad = lagreSøknad()
        client
            .get("/api/soknad/fnr/${søknad.soknadId}")
            .expect(HttpStatusCode.OK, søknad.fnrBruker)
    }

    @Test
    fun `Skal lagre ordre`() = testApplication {
        val søknad = lagreSøknad()
        client
            .post("/api/ordre") { setBody(lagOrdrelinje(søknad)) }
            .expect(HttpStatusCode.OK, 1)
    }

    @Test
    fun `Skal lagre papirsøknad`() = testApplication {
        client
            .post("/api/soknad/papir") { setBody(lagPapirsøknad()) }
            .expect(HttpStatusCode.OK, 1)
    }

    @Test
    fun `Skal lagre vedtaksresultat fra Infotrygd`() = testApplication {
        val søknadId = lagSøknadId()
        val vedtaksresultat1 = lagVedtaksresultat1(søknadId)
        val vedtaksresultat2 = lagVedtaksresultat2(søknadId)
        client
            .post("/api/infotrygd/fagsak") { setBody(vedtaksresultat1) }
            .expect(HttpStatusCode.OK, 1)
        client
            .post("/api/infotrygd/vedtaksresultat") { setBody(vedtaksresultat2) }
            .expect(HttpStatusCode.OK, 1)
        client
            .get("/api/infotrygd/søknadsType/$søknadId")
            .expect(HttpStatusCode.OK, mapOf("søknadsType" to vedtaksresultat2.soknadsType))
        client
            .post("/api/soknad/fra-vedtaksresultat") {
                setBody(
                    SøknadFraVedtaksresultatDto(
                        fnrBruker = vedtaksresultat1.fnrBruker,
                        saksblokkOgSaksnr = vedtaksresultat1.saksblokkOgSaksnr!!,
                        vedtaksdato = vedtaksresultat2.vedtaksdato,
                    ),
                )
            }
            .expect(HttpStatusCode.OK, mapOf("soknadId" to søknadId.toString()))
        client
            .post("/api/soknad/fra-vedtaksresultat-v2") {
                setBody(
                    SøknadFraVedtaksresultatDtoV2(
                        fnrBruker = vedtaksresultat1.fnrBruker,
                        saksblokkOgSaksnr = vedtaksresultat1.saksblokkOgSaksnr!!,
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
        val saksnummer = "1020"
        val søknadId = søknad.soknadId
        val vedtaksresultat = lagVedtaksresultat2(søknadId)
        client
            .post("/api/hotsak/sak") { setBody(HotsakTilknytningData(søknadId, saksnummer)) }
            .expect(HttpStatusCode.OK, 1)
        client
            .post("/api/soknad/hotsak/fra-saknummer") { setBody(SøknadFraHotsakNummerDto(saksnummer)) }
            .expect(HttpStatusCode.OK, mapOf("soknadId" to søknadId.toString()))
        client
            .post("/api/hotsak/vedtaksresultat") { setBody(vedtaksresultat) }
            .expect(HttpStatusCode.OK, 1)
        client
            .post("/api/soknad/hotsak/har-vedtak/fra-søknadid") { setBody(HarVedtakFraHotsakSøknadIdDto(søknadId)) }
            .expect(HttpStatusCode.OK, mapOf("harVedtak" to true))
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
            .put("/api/soknad/status/$søknadId") { setBody(Status.GODKJENT_MED_FULLMAKT) }
            .expect(HttpStatusCode.OK, 1)
        client
            .put("/api/soknad/statusV2") {
                setBody(
                    StatusMedÅrsak(
                        søknadId = søknadId,
                        status = Status.ENDELIG_JOURNALFØRT,
                        valgteÅrsaker = emptySet(),
                        begrunnelse = "begrunnelse",
                    ),
                )
            }
            .expect(HttpStatusCode.OK, 1)
    }

    @Test
    fun `Skal sjekke om søknad finnes`() = testApplication {
        val søknad = lagreSøknad()
        client
            .get("/api/soknad/bruker/finnes/${søknad.soknadId}")
            .expect(HttpStatusCode.OK, "soknadFinnes" to true)
        client
            .get("/api/soknad/bruker/finnes/${lagSøknadId()}")
            .expect(HttpStatusCode.OK, "soknadFinnes" to false)
    }

    @Test
    fun `Skal sjekke om fnr og journalpostId finnes`() = testApplication {
        val søknad = lagreSøknad()
        val journalpostId = 102030
        val dto = FnrOgJournalpostIdFinnesDto(fnrBruker = søknad.fnrBruker, journalpostId = journalpostId)
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
    fun `Skal hente søknadsdata`() = testApplication {
        val søknad = lagreSøknad()
        client.get("/api/soknadsdata/bruker/${søknad.soknadId}")
            .expect<Map<String, Any?>>(HttpStatusCode.OK) {
                it.shouldContain("soknadId", søknad.soknadId.toString())
            }
    }

    @Test
    fun `Hent opprettet dato for søknad`() = testApplication {
        val søknad = lagreSøknad()
        client.get("/api/soknad/opprettet-dato/${søknad.soknadId}")
            .expect<OffsetDateTime>(HttpStatusCode.OK) {
                it.toLocalDate() shouldBe LocalDate.now()
            }
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
    fun `Skal hente behovsmeldingstype for søknad`() = testApplication {
        val søknad = lagreSøknad()
        client.get("/api/soknad/behovsmeldingType/${søknad.soknadId}")
            .expect<Map<String, Any?>>(HttpStatusCode.OK) {
                it.shouldContain("behovsmeldingType", "SØKNAD")
            }
    }

    @Test
    fun `Skal hente søknader for kommune-API-et`() = testApplication {
        val søknad = lagreSøknad()
        client
            .post("/api/kommune-api/soknader") { setBody(mapOf("kommunenummer" to "9999")) }
            .expect<List<SøknadForKommuneApi>>(HttpStatusCode.OK) { søknader ->
                søknader.shouldHaveSingleElement { it.soknadId == søknad.soknadId }
            }
    }

    @Test
    fun `Skal hente datasett for forslagsmotoren`() = testApplication {
        client.get("/api/forslagsmotor/tilbehoer/datasett")
            .expect<Any?>(HttpStatusCode.OK)
    }
}
