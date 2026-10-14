package no.nav.hjelpemidler.soknad.db.test

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.hjelpemidler.http.jackson
import no.nav.hjelpemidler.http.slack.slack
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.soknad.db.azureADRoutes
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.SafselvbetjeningClient
import no.nav.hjelpemidler.soknad.db.felles
import no.nav.hjelpemidler.soknad.db.grunndata.GrunndataClient
import no.nav.hjelpemidler.soknad.db.kafka.kafka
import no.nav.hjelpemidler.soknad.db.metrics.metrics
import no.nav.hjelpemidler.soknad.db.rapportering.epost.EpostClient
import no.nav.hjelpemidler.soknad.db.rolle.RolleClient
import no.nav.hjelpemidler.soknad.db.services
import no.nav.hjelpemidler.soknad.db.store.Database
import no.nav.hjelpemidler.soknad.db.store.database
import no.nav.hjelpemidler.soknad.db.store.testDataSource
import no.nav.hjelpemidler.soknad.db.tokenXRoutes
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import java.time.Clock
import javax.sql.DataSource

fun testApplication(test: suspend TestContext.() -> Unit) = testApplication {
    val context = TestContext(
        client = createClient {
            install(Resources)
            install(RewriteUrl)
            jackson(jsonMapper)
            defaultRequest {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        },
    )

    application {
        dependencies {
            provide<Clock> { Clock.systemDefaultZone() }

            provide<DataSource> { testDataSource }
            database()
            kafka()

            provide<TokenXUserFactory> { context.tokenXUserFactory }
            provide<HttpClientEngine> { MockEngine { respondError(HttpStatusCode.NotImplemented) } }
            provide { slack(engine = resolve()) }

            provide<EpostClient> { context.epostClient }
            provide<GrunndataClient> { context.grunndataClient }
            provide<RolleClient> { context.rolleClient }
            provide<SafselvbetjeningClient> { context.safselvbetjeningClient }

            metrics()
            services()
        }

        val database: Database by dependencies
        database.migrate()

        felles()
        routing {
            route("/api") {
                tokenXRoutes()
                azureADRoutes()
            }
        }
    }

    context.test()
}

private val RewriteUrl = createClientPlugin("RewriteUrl") {
    onRequest { request, _ ->
        request.url {
            if ("api" !in pathSegments) {
                pathSegments = listOf("api") + pathSegments
            }
        }
    }
}
