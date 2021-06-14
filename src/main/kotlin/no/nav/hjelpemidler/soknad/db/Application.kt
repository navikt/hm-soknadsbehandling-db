package no.nav.hjelpemidler.soknad.db

import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.request.path
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.hjelpemidler.soknad.db.db.OrdreStorePostgres
import no.nav.hjelpemidler.soknad.db.db.SøknadStoreFormidlerPostgres
import no.nav.hjelpemidler.soknad.db.db.SøknadStorePostgres
import no.nav.hjelpemidler.soknad.db.db.dataSourceFrom
import no.nav.hjelpemidler.soknad.db.db.migrate
import no.nav.hjelpemidler.soknad.db.db.waitForDB
import no.nav.hjelpemidler.soknad.db.routes.fnrOgJournalpostIdFinnes
import no.nav.hjelpemidler.soknad.db.routes.hentFnrForSoknad
import no.nav.hjelpemidler.soknad.db.routes.hentSoknad
import no.nav.hjelpemidler.soknad.db.routes.hentSoknadOpprettetDato
import no.nav.hjelpemidler.soknad.db.routes.hentSoknaderForBruker
import no.nav.hjelpemidler.soknad.db.routes.hentSoknaderForFormidler
import no.nav.hjelpemidler.soknad.db.routes.hentSoknaderTilGodkjenningEldreEnn
import no.nav.hjelpemidler.soknad.db.routes.hentSoknadsdata
import no.nav.hjelpemidler.soknad.db.routes.hentSøknadIdFraVedtaksresultat
import no.nav.hjelpemidler.soknad.db.routes.lagKnytningMellomFagsakOgSøknad
import no.nav.hjelpemidler.soknad.db.routes.lagreVedtaksresultat
import no.nav.hjelpemidler.soknad.db.routes.oppdaterJournalpostId
import no.nav.hjelpemidler.soknad.db.routes.oppdaterOppgaveId
import no.nav.hjelpemidler.soknad.db.routes.oppdaterStatus
import no.nav.hjelpemidler.soknad.db.routes.ordreSisteDøgn
import no.nav.hjelpemidler.soknad.db.routes.saveOrdrelinje
import no.nav.hjelpemidler.soknad.db.routes.savePapir
import no.nav.hjelpemidler.soknad.db.routes.saveSoknad
import no.nav.hjelpemidler.soknad.db.routes.slettSøknad
import no.nav.hjelpemidler.soknad.db.routes.slettUtløptSøknad
import no.nav.hjelpemidler.soknad.db.routes.soknadFinnes
import no.nav.hjelpemidler.soknad.db.service.hmdb.Hjelpemiddeldatabase
import no.nav.hjelpemidler.soknad.mottak.db.InfotrygdStorePostgres
import org.slf4j.event.Level
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalTime
@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module() {

    val tokenXConfig = runBlocking { loadTokenXConfig() }
    val aadConfig = runBlocking { loadAadConfig() }

    // Last ned hjelpemiddeldatabase datasett for beriking av ordrelinjer
    Hjelpemiddeldatabase.loadDatabase()

    if (!waitForDB(10.minutes, Configuration)) {
        throw Exception("database never became available within the deadline")
    }

    migrate(Configuration)

    val ds: HikariDataSource = dataSourceFrom(Configuration)
    val store = SøknadStorePostgres(ds)
    val storeFormidler = SøknadStoreFormidlerPostgres(ds)
    val ordreStore = OrdreStorePostgres(ds)
    val infotrygdStore = InfotrygdStorePostgres(ds)

    installAuthentication(tokenXConfig, aadConfig, Configuration.application)

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(JacksonMapper.objectMapper))
    }

    install(CallLogging) {
        level = Level.TRACE
        filter { call -> !call.request.path().startsWith("/internal") }
    }

    routing {
        internal()
        route("/api") {

            authenticate("tokenX") {
                hentSoknad(store, ordreStore)
                hentSoknaderForBruker(store)
                hentSoknaderForFormidler(storeFormidler)
            }

            if (Configuration.application.profile == Profile.LOCAL) {
                saveSoknad(store)
                soknadFinnes(store)
                hentFnrForSoknad(store)
                slettSøknad(store)
                slettUtløptSøknad(store)
                oppdaterStatus(store)
                hentSoknadsdata(store)
                hentSoknadOpprettetDato(store)
                hentSoknaderTilGodkjenningEldreEnn(store)
                oppdaterJournalpostId(store)
                oppdaterOppgaveId(store)
                hentSøknadIdFraVedtaksresultat(infotrygdStore)
                saveOrdrelinje(ordreStore)
                lagreVedtaksresultat(infotrygdStore)
                lagKnytningMellomFagsakOgSøknad(infotrygdStore)
                fnrOgJournalpostIdFinnes(store)
                savePapir(store)
                ordreSisteDøgn(ordreStore)
            } else {
                authenticate("aad") {
                    saveSoknad(store)
                    soknadFinnes(store)
                    hentFnrForSoknad(store)
                    slettSøknad(store)
                    slettUtløptSøknad(store)
                    oppdaterStatus(store)
                    hentSoknadsdata(store)
                    hentSoknadOpprettetDato(store)
                    hentSoknaderTilGodkjenningEldreEnn(store)
                    oppdaterJournalpostId(store)
                    oppdaterOppgaveId(store)
                    hentSøknadIdFraVedtaksresultat(infotrygdStore)
                    saveOrdrelinje(ordreStore)
                    lagreVedtaksresultat(infotrygdStore)
                    lagKnytningMellomFagsakOgSøknad(infotrygdStore)
                    fnrOgJournalpostIdFinnes(store)
                    savePapir(store)
                    ordreSisteDøgn(ordreStore)
                }
            }
        }
    }
}
