package no.nav.hjelpemidler.soknad.db

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.engine.apache5.Apache5
import io.ktor.http.ContentType
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.DependencyRegistry
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.provide
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.server.resources.Resources
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.domain.person.TILLAT_SYNTETISKE_FØDSELSNUMRE
import no.nav.hjelpemidler.http.slack.slack
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.SafselvbetjeningClient
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.grunndata.GrunndataClient
import no.nav.hjelpemidler.soknad.db.kafka.kafka
import no.nav.hjelpemidler.soknad.db.metrics.metrics
import no.nav.hjelpemidler.soknad.db.ordre.OrdreService
import no.nav.hjelpemidler.soknad.db.rapportering.Rapporteringsjobber
import no.nav.hjelpemidler.soknad.db.rapportering.epost.EpostClient
import no.nav.hjelpemidler.soknad.db.rapportering.epost.GraphClient
import no.nav.hjelpemidler.soknad.db.rapportering.epost.GraphEpost
import no.nav.hjelpemidler.soknad.db.rapportering.scheduler
import no.nav.hjelpemidler.soknad.db.rolle.RolleClient
import no.nav.hjelpemidler.soknad.db.rolle.RolleService
import no.nav.hjelpemidler.soknad.db.soknad.SøknadService
import no.nav.hjelpemidler.soknad.db.store.Database
import no.nav.hjelpemidler.soknad.db.store.dataSource
import no.nav.hjelpemidler.soknad.db.store.database
import no.nav.tms.token.support.azure.validation.AzureAuthenticator
import no.nav.tms.token.support.azure.validation.azure
import no.nav.tms.token.support.tokenx.validation.TokenXAuthenticator
import no.nav.tms.token.support.tokenx.validation.tokenX
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import org.slf4j.event.Level
import java.time.Clock

private val log = KotlinLogging.logger { }

fun main() {
    embeddedServer(
        factory = Netty,
        port = Configuration.PORT,
        module = Application::main,
    ).start(wait = true)
}

fun Application.main() {
    TILLAT_SYNTETISKE_FØDSELSNUMRE = !Environment.current.isProd

    dependencies {
        provide<Clock> { Clock.systemDefaultZone() }

        dataSource()
        database()
        kafka()

        provide { TokenXUserFactory }
        provide { Apache5.create() }
        provide { slack(engine = resolve()) }

        provide<EpostClient> { GraphEpost(GraphClient(engine = resolve())) }
        provide {
            GrunndataClient(
                engine = resolve(),
                url = Configuration.GRUNNDATA_API_URL,
            )
        }
        provide {
            RolleClient(
                engine = resolve(),
                target = Configuration.HM_ROLLER_TARGET,
                baseUrl = Configuration.HM_ROLLER_URL,
            )
        }
        provide {
            SafselvbetjeningClient(
                engine = resolve(),
                target = Configuration.SAFSELVBETJENING_TARGET,
                baseUrl = Configuration.SAFSELVBETJENING_URL,
            )
        }

        metrics()
        services()
        scheduler()
    }

    val database: Database by dependencies
    database.migrate()

    val rapporteringsjobber: Rapporteringsjobber by dependencies
    rapporteringsjobber.schedulerJobber()

    authentication {
        azure()
        tokenX()
    }

    felles()

    routing {
        internal()
        route("/api") {
            authenticate(AzureAuthenticator.name) {
                azureADRoutes()
            }
            authenticate(TokenXAuthenticator.name) {
                tokenXRoutes()
            }
        }
    }
}

fun DependencyRegistry.services() {
    provide(::OrdreService)
    provide(::RolleService)
    provide(::SøknadService)
}

fun Application.felles() {
    install(Resources)
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(jsonMapper))
    }
    install(CallLogging) {
        level = if (Environment.current.isDev) Level.INFO else Level.TRACE
        filter { call -> call.request.path().startsWith("/api") }
        format { call ->
            "[${call.request.httpMethod.value}] ${call.request.uri}"
        }
    }
    feilmelding()
}
