package no.nav.hjelpemidler.soknad.db

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.resources.Resources
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.database.PostgreSQL
import no.nav.hjelpemidler.database.createDataSource
import no.nav.hjelpemidler.domain.person.TILLAT_SYNTETISKE_FØDSELSNUMRE
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.grunndata.GrunndataClient
import no.nav.hjelpemidler.soknad.db.rolle.RolleClient
import no.nav.hjelpemidler.soknad.db.store.Database
import no.nav.tms.token.support.azure.validation.AzureAuthenticator
import no.nav.tms.token.support.azure.validation.azure
import no.nav.tms.token.support.tokenx.validation.TokenXAuthenticator
import no.nav.tms.token.support.tokenx.validation.tokenX
import org.slf4j.event.Level
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.system.measureTimeMillis

private val logg = KotlinLogging.logger { }

fun main() {
    embeddedServer(Netty, Configuration.PORT, module = Application::module).start(wait = true)
}

fun Application.module() {
    TILLAT_SYNTETISKE_FØDSELSNUMRE = !Environment.current.isProd

    val database = Database(
        createDataSource(PostgreSQL) {
            envVarPrefix = "DB"
        },
    )
    monitor.subscribe(ApplicationStarted) {
        database.migrate()
        monitor.unsubscribe(ApplicationStarted) {}
    }
    monitor.subscribe(ApplicationStopping) {
        database.close()
        monitor.unsubscribe(ApplicationStopping) {}
    }

    val grunndataClient = GrunndataClient()

    val serviceContext = ServiceContext(
        transaction = database,
        grunndataClient = grunndataClient,
        rolleClient = RolleClient(),
    )

    Oppgaveinspektør(database)

    authentication {
        azure()
        tokenX()
    }

    felles()

    routing {
        internal()
        route("/api") {
            authenticate(AzureAuthenticator.name) {
                azureADRoutes(database, serviceContext)
            }
            authenticate(TokenXAuthenticator.name) {
                tokenXRoutes(database, serviceContext)
            }
        }
    }

    startDataMigrering(database, serviceContext)
}

fun Application.felles() {
    install(Resources)
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(jsonMapper))
    }
    install(CallLogging) {
        level = Level.TRACE
        filter { call -> call.request.path().startsWith("/api") }
    }
    feilmelding()
}

private fun startDataMigrering(database: Database, serviceContext: ServiceContext) {
    val timer = Timer("data-migrering-task", true)

    timer.schedule(delay = 10_000) {
        runBlocking(Dispatchers.IO) {
            launch {
                var antallMigrert = 1
                while (antallMigrert > 0) {
                    logg.info { "Kjører datamigreringsbatch." }
                    val tidsbrukMs = measureTimeMillis {
                        antallMigrert = serviceContext.søknadService.migrerTilDataV2()
                    }
                    logg.info { "Antall migrert: $antallMigrert (${tidsbrukMs}ms)" }
                    delay(200)
                }
                logg.info { "Datamigrering ferdig." }
            }
        }
    }
}
