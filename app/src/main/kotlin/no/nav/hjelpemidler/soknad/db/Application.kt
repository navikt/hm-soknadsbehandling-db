package no.nav.hjelpemidler.soknad.db

import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.resources.Resources
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.hjelpemidler.database.PostgreSQL
import no.nav.hjelpemidler.database.createDataSource
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.grunndata.GrunndataClient
import no.nav.hjelpemidler.soknad.db.metrics.Metrics
import no.nav.hjelpemidler.soknad.db.ordre.OrdreService
import no.nav.hjelpemidler.soknad.db.rolle.RolleClient
import no.nav.hjelpemidler.soknad.db.rolle.RolleService
import no.nav.hjelpemidler.soknad.db.store.Database
import no.nav.tms.token.support.azure.validation.AzureAuthenticator
import no.nav.tms.token.support.azure.validation.azure
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder
import no.nav.tms.token.support.tokenx.validation.TokenXAuthenticator
import no.nav.tms.token.support.tokenx.validation.tokenX
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

/**
 * Brukes i application.conf.
 */
@Suppress("unused")
fun Application.module() {
    val database = Database(
        createDataSource(PostgreSQL) {
            envVarPrefix = "DB"
        },
    )
    environment.monitor.subscribe(ApplicationStarted) { database.migrate() }
    environment.monitor.subscribe(ApplicationStopping) { database.close() }

    val grunndataClient = GrunndataClient()

    val ordreService = OrdreService(database, grunndataClient)
    val tokendingsService = TokendingsServiceBuilder.buildTokendingsService()
    val rolleService = RolleService(RolleClient(tokendingsService))

    val metrics = Metrics(database)

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
                azureADRoutes(database, metrics)
                kommuneApi(database)
            }
            authenticate(TokenXAuthenticator.name) {
                tokenXRoutes(database, ordreService, rolleService)
            }
        }
    }
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
