package no.nav.hjelpemidler.soknad.db.test

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.hjelpemidler.http.createHttpClient
import no.nav.hjelpemidler.soknad.db.ServiceContext
import no.nav.hjelpemidler.soknad.db.azureADRoutes
import no.nav.hjelpemidler.soknad.db.felles
import no.nav.hjelpemidler.soknad.db.jsonMapper
import no.nav.hjelpemidler.soknad.db.store.testDatabase
import no.nav.hjelpemidler.soknad.db.tokenXRoutes

fun testApplication(test: suspend TestContext.() -> Unit) = testApplication {
    val database = testDatabase.apply { migrate() }
    val context = TestContext(
        createHttpClient(client.engine, jsonMapper) {
            install(Resources)
            install(RewriteUrl)
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
        felles()

        val serviceContext = ServiceContext(
            transaction = database,
            grunndataClient = context.grunndataClient,
            rolleClient = context.rolleClient,
            metrics = context.metrics,
        )

        routing {
            route("/api") {
                tokenXRoutes(
                    transaction = database,
                    serviceContext = serviceContext,
                    tokenXUserFactory = context.tokenXUserFactory,
                )
                azureADRoutes(
                    transaction = database,
                    serviceContext = serviceContext,
                )
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
