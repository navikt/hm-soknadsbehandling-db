package no.nav.hjelpemidler.soknad.db

import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.configuration.LocalEnvironment
import no.nav.hjelpemidler.soknad.db.db.HotsakStorePostgres
import no.nav.hjelpemidler.soknad.db.db.OrdreStorePostgres
import no.nav.hjelpemidler.soknad.db.db.SøknadStoreInnsenderPostgres
import no.nav.hjelpemidler.soknad.db.db.SøknadStorePostgres
import no.nav.hjelpemidler.soknad.db.db.dataSourceFrom
import no.nav.hjelpemidler.soknad.db.db.migrate
import no.nav.hjelpemidler.soknad.db.db.waitForDB
import no.nav.hjelpemidler.soknad.db.metrics.Metrics
import no.nav.hjelpemidler.soknad.db.rolle.RolleClient
import no.nav.hjelpemidler.soknad.db.rolle.RolleService
import no.nav.hjelpemidler.soknad.db.routes.azureAdRoutes
import no.nav.hjelpemidler.soknad.db.routes.tokenXRoutes
import no.nav.hjelpemidler.soknad.mottak.db.InfotrygdStorePostgres
import no.nav.tms.token.support.azure.validation.AzureAuthenticator
import no.nav.tms.token.support.azure.validation.azure
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder
import no.nav.tms.token.support.tokenx.validation.TokenXAuthenticator
import no.nav.tms.token.support.tokenx.validation.tokenX
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalTime
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    if (!waitForDB(10.minutes, Configuration)) {
        throw Exception("database never became available within the deadline")
    }

    migrate(Configuration)

    val dataSource: HikariDataSource = dataSourceFrom(Configuration)
    val søknadStore = SøknadStorePostgres(dataSource)
    val storeFormidler = SøknadStoreInnsenderPostgres(dataSource)
    val ordreStore = OrdreStorePostgres(dataSource)
    val infotrygdStore = InfotrygdStorePostgres(dataSource)
    val hotsakStore = HotsakStorePostgres(dataSource)
    val metrics = Metrics(søknadStore)
    val tokendingsService = TokendingsServiceBuilder.buildTokendingsService()
    val rolleService = RolleService(RolleClient(tokendingsService))

    Oppgaveinspektør(søknadStore)

    authentication {
        azure()
        tokenX()
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(JacksonMapper.objectMapper))
    }

    install(CallLogging) {
        level = Level.TRACE
        filter { call -> call.request.path().startsWith("/api") }
    }

    routing {
        internal()
        route("/api") {
            authenticate(TokenXAuthenticator.name) {
                tokenXRoutes(søknadStore, ordreStore, infotrygdStore, hotsakStore, storeFormidler, rolleService)
            }

            when (Environment.current) {
                LocalEnvironment -> azureAdRoutes(
                    søknadStore,
                    ordreStore,
                    infotrygdStore,
                    hotsakStore,
                    metrics,
                )

                else -> authenticate(AzureAuthenticator.name) {
                    azureAdRoutes(
                        søknadStore,
                        ordreStore,
                        infotrygdStore,
                        hotsakStore,
                        metrics,
                    )
                }
            }
        }
    }
}
