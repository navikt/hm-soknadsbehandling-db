package no.nav.hjelpemidler.soknad.db.test

import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.hjelpemidler.http.createHttpClient
import no.nav.hjelpemidler.soknad.db.azureADRoutes
import no.nav.hjelpemidler.soknad.db.jackson
import no.nav.hjelpemidler.soknad.db.jsonMapper
import no.nav.hjelpemidler.soknad.db.ordre.OrdreService
import no.nav.hjelpemidler.soknad.db.rolle.RolleService
import no.nav.hjelpemidler.soknad.db.store.testDatabase
import no.nav.hjelpemidler.soknad.db.tokenXRoutes

fun testApplication(test: suspend TestContext.() -> Unit) = testApplication {
    val database = testDatabase.apply { migrate() }
    val context = TestContext(
        createHttpClient(client.engine, jsonMapper) {
            defaultRequest {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        },
    )

    environment {
        config = MapApplicationConfig()
    }

    application {
        jackson()

        routing {
            route("/api") {
                tokenXRoutes(
                    transaction = database,
                    ordreService = OrdreService(database, context.grunndataClient),
                    rolleService = RolleService(context.rolleClient),
                    tokenXUserFactory = context.tokenXUserFactory,
                )
                azureADRoutes(
                    transaction = database,
                    metrics = context.metrics,
                )
            }
        }
    }

    context.test()
}
