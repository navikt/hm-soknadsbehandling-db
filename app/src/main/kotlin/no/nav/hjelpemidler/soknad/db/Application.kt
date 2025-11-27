package no.nav.hjelpemidler.soknad.db

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.engine.apache.Apache
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
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.database.PostgreSQL
import no.nav.hjelpemidler.database.createDataSource
import no.nav.hjelpemidler.domain.person.TILLAT_SYNTETISKE_FØDSELSNUMRE
import no.nav.hjelpemidler.http.openid.entraIDClient
import no.nav.hjelpemidler.http.slack.slack
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.soknad.db.exception.feilmelding
import no.nav.hjelpemidler.soknad.db.grunndata.GrunndataClient
import no.nav.hjelpemidler.soknad.db.metrics.kafka.createKafkaClient
import no.nav.hjelpemidler.soknad.db.rapportering.JobbScheduler
import no.nav.hjelpemidler.soknad.db.rapportering.ManglendeBrukerbekreftelse
import no.nav.hjelpemidler.soknad.db.rapportering.ManglendeOppgaver
import no.nav.hjelpemidler.soknad.db.rapportering.NaisLeaderElection
import no.nav.hjelpemidler.soknad.db.rapportering.Rapporteringsjobber
import no.nav.hjelpemidler.soknad.db.rapportering.epost.GraphClient
import no.nav.hjelpemidler.soknad.db.rapportering.epost.GraphEpost
import no.nav.hjelpemidler.soknad.db.rolle.RolleClient
import no.nav.hjelpemidler.soknad.db.safselvbetjening.Safselvbetjening
import no.nav.hjelpemidler.soknad.db.store.Database
import no.nav.tms.token.support.azure.validation.AzureAuthenticator
import no.nav.tms.token.support.azure.validation.azure
import no.nav.tms.token.support.tokenx.validation.TokenXAuthenticator
import no.nav.tms.token.support.tokenx.validation.tokenX
import org.slf4j.event.Level
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

private val logg = KotlinLogging.logger { }

fun main() {
    embeddedServer(Netty, Configuration.PORT, module = Application::module).start(wait = true)
}

fun Application.module() {
    TILLAT_SYNTETISKE_FØDSELSNUMRE = !Environment.current.isProd
    val clock = Clock.systemDefaultZone()

    val database = Database(
        createDataSource(PostgreSQL) {
            envVarPrefix = "DB"
        },
        clock = clock,
    )

    val entraIDClient = entraIDClient {
        cache(leeway = 10.seconds) {
            maximumSize = 100
        }
    }
    val slack = slack(engine = Apache.create())
    val epostClient = GraphEpost(GraphClient(entraIDClient))
    val leaderElection = NaisLeaderElection()
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    val jobbScheduler = JobbScheduler(scheduler, leaderElection, slack)
    val rapporteringsjobber = Rapporteringsjobber(
        jobbScheduler,
        ManglendeOppgaver(database, slack),
        ManglendeBrukerbekreftelse(database, epostClient, clock),
        epostClient,
    )

    monitor.subscribe(ApplicationStarted) {
        database.migrate()
        rapporteringsjobber.schedulerJobber()
        monitor.unsubscribe(ApplicationStarted) {}
    }
    monitor.subscribe(ApplicationStopping) {
        database.close()
        scheduler.awaitTermination(10, TimeUnit.SECONDS)
        monitor.unsubscribe(ApplicationStopping) {}
    }

    val kafkaClient = createKafkaClient()
    val grunndataClient = GrunndataClient()

    val serviceContext = ServiceContext(
        transaction = database,
        grunndataClient = grunndataClient,
        rolleClient = RolleClient(),
        safselvbetjening = Safselvbetjening(),
        kafkaClient = kafkaClient,
    )

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
