package no.nav.hjelpemidler.soknad.db

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.hjelpemidler.soknad.db.metrics.Prometheus

fun Route.internal() {
    get("/is_alive") {
        call.respondText("ALIVE", ContentType.Text.Plain)
    }
    get("/is_ready") {
        call.respondText("READY", ContentType.Text.Plain)
    }
    get("/metrics") {
        call.respond(Prometheus.registry.scrape())
    }
}
