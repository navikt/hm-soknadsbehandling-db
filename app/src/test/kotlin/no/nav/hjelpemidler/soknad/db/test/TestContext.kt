package no.nav.hjelpemidler.soknad.db.test

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.Behovsmeldingsgrunnlag
import no.nav.hjelpemidler.behovsmeldingsmodell.Statusendring
import no.nav.hjelpemidler.behovsmeldingsmodell.SøknadDto
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Fagsak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Sakstilknytning
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Vedtaksresultat
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Innsenderbehovsmelding
import no.nav.hjelpemidler.domain.person.Fødselsnummer
import no.nav.hjelpemidler.domain.person.lagTilfeldigFødselsnummer
import no.nav.hjelpemidler.domain.tilgang.Applikasjonsnavn
import no.nav.hjelpemidler.security.AuthenticatedApplication
import no.nav.hjelpemidler.server.ktor.security.test.bearerAuth
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.SafselvbetjeningClient
import no.nav.hjelpemidler.soknad.db.grunndata.GrunndataClient
import no.nav.hjelpemidler.soknad.db.grunndata.hentprodukter.AttributesDoc
import no.nav.hjelpemidler.soknad.db.grunndata.hentprodukter.Product
import no.nav.hjelpemidler.soknad.db.rolle.FormidlerRolle
import no.nav.hjelpemidler.soknad.db.rolle.RolleClient
import no.nav.hjelpemidler.soknad.db.rolle.RolleResultat
import no.nav.hjelpemidler.soknad.db.soknad.Behovsmelding
import no.nav.hjelpemidler.soknad.db.soknad.Søknader
import no.nav.hjelpemidler.soknad.db.soknad.lagBehovsmeldingsgrunnlagDigital
import no.nav.hjelpemidler.soknad.db.test.fakes.FakeEpostClient

class TestContext(
    val client: HttpClient,
    val epostClient: FakeEpostClient = FakeEpostClient(),
    val grunndataClient: GrunndataClient = mockk(),
    val rolleClient: RolleClient = mockk(),
    val safselvbetjeningClient: SafselvbetjeningClient = mockk(),
) {
    val systembruker = AuthenticatedApplication(Applikasjonsnavn("test"))

    init {
        coEvery { grunndataClient.hentProdukter(any()) } answers {
            firstArg<Set<String>>().map {
                Product(
                    hmsArtNr = it,
                    articleName = "test",
                    isoCategoryTitle = null,
                    productVariantURL = "http://grunndata/artikler/$it",
                    attributes = AttributesDoc(""),
                    media = emptyList(),
                )
            }
        }
    }

    fun formidlerRolle() {
        coEvery { rolleClient.hentRolle(any()) } returns RolleResultat(
            formidlerRolle = FormidlerRolle(
                harFormidlerRolle = true,
                erPilotkommune = false,
                harAltinnRettighet = true,
                harAllowlistTilgang = true,
                organisasjoner = emptyList(),
                organisasjonerManKanBeOmTilgangTil = emptyList(),
                godkjenningskurs = emptyList(),
                feil = emptyList(),
            ),
        )
    }

    suspend inline fun lagreBehovsmelding(
        fnrBruker: Fødselsnummer = lagTilfeldigFødselsnummer(),
        fnrInnsender: Fødselsnummer = lagTilfeldigFødselsnummer(),
    ): Behovsmeldingsgrunnlag.Digital {
        return lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital(fnrBruker = fnrBruker, fnrInnsender = fnrInnsender))
    }

    suspend inline fun <reified T : Behovsmeldingsgrunnlag> lagreBehovsmelding(grunnlag: T, rowUpdated: Int = 1): T {
        client
            .post(Søknader()) {
                bearerAuth(systembruker)
                setBody(grunnlag)
            }
            .expect(HttpStatusCode.Created, rowUpdated)
        return grunnlag
    }

    suspend fun oppdaterStatus(søknadId: BehovsmeldingId, status: BehovsmeldingStatus) {
        client
            .put(Søknader.SøknadId.Status(søknadId)) {
                bearerAuth(systembruker)
                setBody(Statusendring(status = status, valgteÅrsaker = null, begrunnelse = null))
            }
            .expect(HttpStatusCode.OK, 1)
    }

    suspend fun finnSøknad(søknadId: BehovsmeldingId): SøknadDto? {
        val response = client.get(Søknader.SøknadId(søknadId)) {
            bearerAuth(systembruker)
        }
        response shouldHaveStatus HttpStatusCode.OK
        return response.body<SøknadDto?>()
    }

    suspend fun finnBehovsmelding(søknadId: BehovsmeldingId): Innsenderbehovsmelding? {
        val response = client.get(Behovsmelding.BehovsmeldingId(søknadId)) {
            bearerAuth(systembruker)
        }
        response shouldHaveStatus HttpStatusCode.OK
        return response.body<Innsenderbehovsmelding?>()
    }

    suspend inline fun <reified T : Fagsak> finnSak(søknadId: BehovsmeldingId): T? {
        val response = client.get(Søknader.SøknadId.Sak(søknadId)) {
            bearerAuth(systembruker)
        }
        response shouldHaveStatus HttpStatusCode.OK
        return response.body<T?>()
    }

    suspend fun oppdaterJournalpostId(
        søknadId: BehovsmeldingId,
        journalpostId: String,
    ) {
        client
            .put(Søknader.SøknadId.Journalpost(søknadId)) {
                bearerAuth(systembruker)
                setBody(mapOf("journalpostId" to journalpostId))
            }
            .expect(HttpStatusCode.OK, 1)
    }

    suspend fun oppdaterOppgaveId(søknadId: BehovsmeldingId, oppgaveId: String) {
        client
            .put(Søknader.SøknadId.Oppgave(søknadId)) {
                bearerAuth(systembruker)
                setBody(mapOf("oppgaveId" to oppgaveId))
            }
            .expect(HttpStatusCode.OK, 1)
    }

    suspend fun lagreSakstilknytning(
        søknadId: BehovsmeldingId,
        sakstilknytning: Sakstilknytning,
    ) {
        client
            .post(Søknader.SøknadId.Sak(søknadId)) {
                bearerAuth(systembruker)
                setBody(sakstilknytning)
            }
            .expect(HttpStatusCode.OK, 1)
    }

    suspend fun lagreVedtaksresultat(
        søknadId: BehovsmeldingId,
        vedtaksresultat: Vedtaksresultat,
    ) {
        client
            .post(Søknader.SøknadId.Vedtaksresultat(søknadId)) {
                bearerAuth(systembruker)
                setBody(vedtaksresultat)
            }
            .expect(HttpStatusCode.OK, 1)
    }
}
