package no.nav.hjelpemidler.soknad.db

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonAnySetter
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.resources.get
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSakId
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Sakstilknytning
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Vedtaksresultat
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import no.nav.hjelpemidler.soknad.db.soknad.Søknader
import no.nav.hjelpemidler.soknad.db.test.expect
import no.nav.hjelpemidler.soknad.db.test.feilmelding
import no.nav.hjelpemidler.soknad.db.test.testApplication
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class TokenXRoutesTest {
    @Test
    fun `Hent søknad med søknadId for bruker`() = testApplication {
        val grunnlag = lagreBehovsmelding()
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

        tokenXUser(grunnlag.fnrBruker)

        client
            .get(Søknader.Bruker.SøknadId(grunnlag.søknadId))
            .expect<SøknadDto>(HttpStatusCode.OK) {
                it.søknadId shouldBe grunnlag.søknadId
                it.andre.shouldContain("fagsakId", sakId.toString())
                it.andre.shouldContain("søknadType", søknadstype)
            }
    }

    @Test
    fun `Hent søknad med søknadId for bruker feiler`() = testApplication {
        val grunnlag = lagreBehovsmelding()
        val melding = "Noe gikk galt!"

        createTokenXUserFeiler(melding)

        client
            .get(Søknader.Bruker.SøknadId(grunnlag.søknadId))
            .feilmelding(HttpStatusCode.InternalServerError) {
                it.message shouldBe melding
            }
    }

    @Test
    fun `Hent søknad med søknadId for bruker uten tilgang`() = testApplication {
        val grunnlag = lagreBehovsmelding()

        tokenXUser(lagFødselsnummer())

        client
            .get(Søknader.Bruker.SøknadId(grunnlag.søknadId))
            .feilmelding(HttpStatusCode.Forbidden) {
                it.message shouldBe "Søknad er ikke registrert på aktuell bruker"
            }
    }

    @Test
    fun `Hent søknader for bruker`() = testApplication {
        val grunnlag = lagreBehovsmelding()

        tokenXUser(grunnlag.fnrBruker)

        client
            .get(Søknader.Bruker())
            .expect<List<SøknadDto>>(HttpStatusCode.OK) { søknader ->
                søknader.shouldBeSingleton {
                    it.søknadId shouldBe grunnlag.søknadId
                }
            }
    }

    @Test
    fun `Hent søknad med søknadId for innsender`() = testApplication {
        val grunnlag = lagreBehovsmelding()

        tokenXUser(grunnlag.fnrInnsender)
        formidlerRolle()

        client
            .get(Søknader.Innsender.SøknadId(grunnlag.søknadId))
            .expect<SøknadDto>(HttpStatusCode.OK) {
                it.søknadId shouldBe grunnlag.søknadId
            }
    }

    @Test
    fun `Hent søknader for innsender`() = testApplication {
        val grunnlag = lagreBehovsmelding()

        tokenXUser(grunnlag.fnrInnsender)
        formidlerRolle()

        client
            .get(Søknader.Innsender())
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
