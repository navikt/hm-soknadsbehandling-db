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
import kotlinx.coroutines.runBlocking
import no.nav.hjelpemidler.soknad.db.db.InfotrygdStorePostgres
import no.nav.hjelpemidler.soknad.db.db.OrdreStorePostgres
import no.nav.hjelpemidler.soknad.db.db.SøknadStoreFormidlerPostgres
import no.nav.hjelpemidler.soknad.db.db.SøknadStorePostgres
import no.nav.hjelpemidler.soknad.db.db.dataSourceFrom
import no.nav.hjelpemidler.soknad.db.db.waitForDB
import no.nav.hjelpemidler.soknad.db.routes.hentSoknad
import no.nav.hjelpemidler.soknad.db.routes.hentSoknaderForBruker
import no.nav.hjelpemidler.soknad.db.routes.hentSoknaderForFormidler
import org.slf4j.event.Level
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalTime
fun Application.module(testing: Boolean = false) {

    val tokenXConfig = runBlocking { loadTokenXConfig() }
    val aadConfig = runBlocking { loadAadConfig() }

    if (!waitForDB(10.minutes, Configuration)) {
        throw Exception("database never became available within the deadline")
    }

    val ds: HikariDataSource = dataSourceFrom(Configuration)
    val store = SøknadStorePostgres(ds)
    val storeFormidler = SøknadStoreFormidlerPostgres(ds)
    val ordreStore = OrdreStorePostgres(ds)
    val infotrygdStore = InfotrygdStorePostgres(ds)

    installAuthentication(tokenXConfig, aadConfig, Configuration.application)

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter())
    }

    install(CallLogging) {
        level = Level.TRACE
        filter { call -> !call.request.path().startsWith("/internal") }
    }

    routing {
        route("/api") {
            authenticate("tokenX") {
                hentSoknad(store)
                hentSoknaderForBruker(store)
                hentSoknaderForFormidler(storeFormidler)
            }

            authenticate("aad") {
                hentSoknad(store)
            }
        }
    }
}
