package no.nav.hjelpemidler.soknad.db.test

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.hjelpemidler.soknad.db.client.hmdb.hentproduktermedhmsnrs.AttributesDoc
import no.nav.hjelpemidler.soknad.db.client.hmdb.hentproduktermedhmsnrs.Product
import no.nav.hjelpemidler.soknad.db.domain.Søknad
import no.nav.hjelpemidler.soknad.db.domain.SøknadData
import no.nav.hjelpemidler.soknad.db.domain.SøknadId
import no.nav.hjelpemidler.soknad.db.domain.lagSøknad
import no.nav.hjelpemidler.soknad.db.grunndata.GrunndataClient
import no.nav.hjelpemidler.soknad.db.metrics.Metrics
import no.nav.hjelpemidler.soknad.db.rolle.FormidlerRolle
import no.nav.hjelpemidler.soknad.db.rolle.RolleClient
import no.nav.hjelpemidler.soknad.db.rolle.RolleResultat
import no.nav.hjelpemidler.soknad.db.sak.Sakstilknytning
import no.nav.hjelpemidler.soknad.db.sak.Vedtaksresultat
import no.nav.hjelpemidler.soknad.db.soknad.Søknader
import no.nav.tms.token.support.tokenx.validation.LevelOfAssurance
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import java.time.Instant

class TestContext(
    val client: HttpClient,
    val grunndataClient: GrunndataClient = mockk(),
    val metrics: Metrics = mockk(relaxed = true),
    val rolleClient: RolleClient = mockk(),
    val tokenXUserFactory: TokenXUserFactory = mockk(),
) {
    init {
        coEvery { grunndataClient.hentProdukterMedHmsnrs(any()) } answers {
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

    fun tokenXUser(ident: String? = null) {
        every { tokenXUserFactory.createTokenXUser(any()) } returns TokenXUser(
            ident = ident ?: "12345678910",
            loginLevel = 3,
            levelOfAssurance = LevelOfAssurance.SUBSTANTIAL,
            tokenExpirationTime = Instant.now().plusSeconds(60),
            jwt = lagJwt(),
        )
    }

    fun createTokenXUserFeiler(melding: String = "Ukjent feil!") {
        every { tokenXUserFactory.createTokenXUser(any()) } throws RuntimeException(melding)
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

    fun hentRolleFeiler(melding: String = "Ukjent feil!") {
        coEvery { rolleClient.hentRolle(any()) } throws RuntimeException(melding)
    }

    suspend fun lagreSøknad(søknad: SøknadData = lagSøknad()): SøknadData {
        client
            .post("/api/soknad/bruker") { setBody(søknad) }
            .expect(HttpStatusCode.Created, 1)
        return søknad
    }

    suspend fun hentSøknad(søknadId: SøknadId): Søknad {
        val response = client.get(Søknader.SøknadId(søknadId))
        response shouldHaveStatus HttpStatusCode.OK
        return response.body()
    }

    // fixme -> ny url
    suspend fun oppdaterJournalpostId(søknadId: SøknadId, journalpostId: String) {
        client.put("/api/soknad/journalpost-id/$søknadId") {
            setBody(mapOf("journalpostId" to journalpostId))
        } shouldHaveStatus HttpStatusCode.OK
    }

    // fixme -> ny url
    suspend fun oppdaterOppgaveId(søknadId: SøknadId, oppgaveId: String) {
        client.put("/api/soknad/oppgave-id/$søknadId") {
            setBody(mapOf("oppgaveId" to oppgaveId))
        } shouldHaveStatus HttpStatusCode.OK
    }

    suspend fun lagreSakstilknytning(søknadId: SøknadId, sakstilknytning: Sakstilknytning) {
        client
            .post(Søknader.SøknadId.Sak(søknadId)) {
                setBody(sakstilknytning)
            }
            .expect(HttpStatusCode.OK, 1)
    }

    suspend fun lagreVedtaksresultat(søknadId: SøknadId, vedtaksresultat: Vedtaksresultat) {
        client
            .post(Søknader.SøknadId.Vedtaksresultat(søknadId)) {
                setBody(vedtaksresultat)
            }
            .expect(HttpStatusCode.OK, 1)
    }
}
