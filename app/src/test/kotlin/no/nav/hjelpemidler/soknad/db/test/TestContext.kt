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
import io.mockk.every
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
import no.nav.hjelpemidler.soknad.db.client.hmdb.hentproduktermedhmsnrs.AttributesDoc
import no.nav.hjelpemidler.soknad.db.client.hmdb.hentproduktermedhmsnrs.Product
import no.nav.hjelpemidler.soknad.db.grunndata.GrunndataClient
import no.nav.hjelpemidler.soknad.db.metrics.Metrics
import no.nav.hjelpemidler.soknad.db.rolle.FormidlerRolle
import no.nav.hjelpemidler.soknad.db.rolle.RolleClient
import no.nav.hjelpemidler.soknad.db.rolle.RolleResultat
import no.nav.hjelpemidler.soknad.db.soknad.Behovsmelding
import no.nav.hjelpemidler.soknad.db.soknad.Søknader
import no.nav.hjelpemidler.soknad.db.soknad.lagBehovsmeldingsgrunnlagDigital
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

    suspend fun lagreBehovsmelding(): Behovsmeldingsgrunnlag.Digital {
        return lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital())
    }

    suspend fun <T : Behovsmeldingsgrunnlag> lagreBehovsmelding(grunnlag: T, rowUpdated: Int = 1): T {
        client
            .post(Søknader()) { setBody<Behovsmeldingsgrunnlag>(grunnlag) }
            .expect(HttpStatusCode.Created, rowUpdated)
        return grunnlag
    }

    suspend fun oppdaterStatus(søknadId: BehovsmeldingId, status: BehovsmeldingStatus) {
        client
            .put(Søknader.SøknadId.Status(søknadId)) {
                setBody(Statusendring(status = status, valgteÅrsaker = null, begrunnelse = null))
            }
            .expect(HttpStatusCode.OK, 1)
    }

    suspend fun finnSøknad(søknadId: BehovsmeldingId, inkluderData: Boolean = false): SøknadDto? {
        val response = client.get(Søknader.SøknadId(søknadId, inkluderData))
        response shouldHaveStatus HttpStatusCode.OK
        return response.body<SøknadDto?>()
    }

    suspend fun finnBehovsmelding(søknadId: BehovsmeldingId): Innsenderbehovsmelding? {
        val response = client.get(Behovsmelding.BehovsmeldingId(søknadId))
        response shouldHaveStatus HttpStatusCode.OK
        return response.body<Innsenderbehovsmelding?>()
    }

    suspend fun finnSak(søknadId: BehovsmeldingId): Fagsak? {
        val response = client.get(Søknader.SøknadId.Sak(søknadId))
        response shouldHaveStatus HttpStatusCode.OK
        return response.body<Fagsak?>()
    }

    suspend fun oppdaterJournalpostId(
        søknadId: BehovsmeldingId,
        journalpostId: String,
    ) {
        client
            .put(Søknader.SøknadId.Journalpost(søknadId)) {
                setBody(mapOf("journalpostId" to journalpostId))
            }
            .expect(HttpStatusCode.OK, 1)
    }

    suspend fun oppdaterOppgaveId(søknadId: BehovsmeldingId, oppgaveId: String) {
        client
            .put(Søknader.SøknadId.Oppgave(søknadId)) {
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
                setBody(vedtaksresultat)
            }
            .expect(HttpStatusCode.OK, 1)
    }
}
