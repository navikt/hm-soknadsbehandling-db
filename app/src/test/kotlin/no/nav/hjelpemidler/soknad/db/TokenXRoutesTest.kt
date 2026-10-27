package no.nav.hjelpemidler.soknad.db

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonAnySetter
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.resources.get
import io.ktor.client.request.bearerAuth
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSakId
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Sakstilknytning
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Vedtaksresultat
import no.nav.hjelpemidler.server.ktor.security.test.lagAutentisertInnbygger
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import no.nav.hjelpemidler.soknad.db.soknad.Søknader
import no.nav.hjelpemidler.soknad.db.test.expect
import no.nav.hjelpemidler.soknad.db.test.feilmelding
import no.nav.hjelpemidler.soknad.db.test.testApplication
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class TokenXRoutesTest {
    private val bruker = lagAutentisertInnbygger()
    private val innsender = lagAutentisertInnbygger()

    @Test
    fun `Hent søknad med søknadId for bruker`() = testApplication {
        val grunnlag = lagreBehovsmelding(fnrBruker = bruker.fnr, fnrInnsender = innsender.fnr)
        val sakId = InfotrygdSakId("9999A01")
        val søknadstype = "TEST"
        lagreSakstilknytning(
            grunnlag.søknadId,
            Sakstilknytning.Infotrygd(sakId, grunnlag.fnrBruker),
        )
        lagreVedtaksresultat(
            grunnlag.søknadId,
            Vedtaksresultat.Infotrygd("I", LocalDate.now(), søknadstype),
        )

        client
            .get(Søknader.Bruker.SøknadId(grunnlag.søknadId)) {
                bearerAuth(bruker.userToken)
            }
            .expect<SøknadDto>(HttpStatusCode.OK) {
                it.søknadId shouldBe grunnlag.søknadId
                it.andre.shouldContain("fagsakId", sakId.toString())
                it.andre.shouldContain("søknadType", søknadstype)
            }
    }

    @Test
    fun `Hent søknad med søknadId for bruker feiler`() = testApplication {
        val grunnlag = lagreBehovsmelding(fnrBruker = bruker.fnr, fnrInnsender = innsender.fnr)

        client
            .get(Søknader.Bruker.SøknadId(grunnlag.søknadId))
            .status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `Hent søknad med søknadId for bruker uten tilgang`() = testApplication {
        val grunnlag = lagreBehovsmelding(fnrBruker = lagFødselsnummer(), fnrInnsender = innsender.fnr)

        client
            .get(Søknader.Bruker.SøknadId(grunnlag.søknadId)) {
                bearerAuth(bruker.userToken)
            }
            .feilmelding(HttpStatusCode.Forbidden) {
                it.message shouldBe "Søknad er ikke registrert på aktuell bruker"
            }
    }

    @Test
    fun `Hent søknader for bruker`() = testApplication {
        val grunnlag = lagreBehovsmelding(fnrBruker = bruker.fnr, fnrInnsender = innsender.fnr)

        client
            .get(Søknader.Bruker()) {
                bearerAuth(bruker.userToken)
            }
            .expect<List<SøknadDto>>(HttpStatusCode.OK) { søknader ->
                søknader.shouldBeSingleton {
                    it.søknadId shouldBe grunnlag.søknadId
                }
            }
    }

    @Test
    fun `Hent søknad med søknadId for innsender`() = testApplication {
        val grunnlag = lagreBehovsmelding(fnrBruker = bruker.fnr, fnrInnsender = innsender.fnr)

        formidlerRolle()

        client
            .get(Søknader.Innsender.SøknadId(grunnlag.søknadId)) {
                bearerAuth(innsender.userToken)
            }
            .expect<SøknadDto>(HttpStatusCode.OK) {
                it.søknadId shouldBe grunnlag.søknadId
            }
    }

    @Test
    fun `Hent søknader for innsender`() = testApplication {
        val grunnlag = lagreBehovsmelding(fnrBruker = bruker.fnr, fnrInnsender = innsender.fnr)

        formidlerRolle()

        client
            .get(Søknader.Innsender()) {
                bearerAuth(innsender.userToken)
            }
            .expect<List<SøknadDto>>(HttpStatusCode.OK) { søknader ->
                søknader.shouldBeSingleton {
                    it.søknadId shouldBe grunnlag.søknadId
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
