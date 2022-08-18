package no.nav.hjelpemidler.soknad.db

import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import kotlinx.coroutines.runBlocking
import no.nav.hjelpemidler.soknad.db.db.HotsakStorePostgres
import no.nav.hjelpemidler.soknad.db.db.MidlertidigPrisforhandletTilbehoerStorePostgres
import no.nav.hjelpemidler.soknad.db.db.OrdreStorePostgres
import no.nav.hjelpemidler.soknad.db.db.SøknadStoreFormidlerPostgres
import no.nav.hjelpemidler.soknad.db.db.SøknadStorePostgres
import no.nav.hjelpemidler.soknad.db.db.dataSourceFrom
import no.nav.hjelpemidler.soknad.db.db.migrate
import no.nav.hjelpemidler.soknad.db.db.waitForDB
import no.nav.hjelpemidler.soknad.db.routes.azureAdRoutes
import no.nav.hjelpemidler.soknad.db.routes.tokenXRoutes
import no.nav.hjelpemidler.soknad.mottak.db.InfotrygdStorePostgres
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalTime
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    val tokenXConfig = runBlocking { loadTokenXConfig() }
    val aadConfig = runBlocking { loadAadConfig() }

    if (!waitForDB(10.minutes, Configuration)) {
        throw Exception("database never became available within the deadline")
    }

    migrate(Configuration)

    val dataSource: HikariDataSource = dataSourceFrom(Configuration)
    val søknadStore = SøknadStorePostgres(dataSource)
    val storeFormidler = SøknadStoreFormidlerPostgres(dataSource)
    val ordreStore = OrdreStorePostgres(dataSource)
    val infotrygdStore = InfotrygdStorePostgres(dataSource)
    val hotsakStore = HotsakStorePostgres(dataSource)
    val midlertidigPrisforhandletTilbehoerStorePostgres = MidlertidigPrisforhandletTilbehoerStorePostgres(dataSource)

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
                tokenXRoutes(søknadStore, ordreStore, infotrygdStore, hotsakStore, storeFormidler)
            }

            when (Configuration.application.profile) {
                Profile.LOCAL -> {
                    azureAdRoutes(
                        søknadStore,
                        ordreStore,
                        infotrygdStore,
                        hotsakStore,
                        midlertidigPrisforhandletTilbehoerStorePostgres
                    )
                }
                else -> {
                    authenticate("aad") {
                        azureAdRoutes(
                            søknadStore,
                            ordreStore,
                            infotrygdStore,
                            hotsakStore,
                            midlertidigPrisforhandletTilbehoerStorePostgres
                        )
                    }
                }
            }

            // FIXME: fjern igjen når test er over
            get("/hentStatistikkOversiktForPrisforhandletTilbehoer") {
                call.respond(midlertidigPrisforhandletTilbehoerStorePostgres.hentOversikt())
            }
        }
    }
}
