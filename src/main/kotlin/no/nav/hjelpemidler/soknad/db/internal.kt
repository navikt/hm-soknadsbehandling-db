package no.nav.hjelpemidler.soknad.db

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.Route
import io.ktor.routing.get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import javax.sql.DataSource

fun Route.internal(ds: DataSource) {
    get("/is_alive") {
        if (ds.connection.isClosed) {
            return@get call.respondText("NOT ALIVE", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
        }
        call.respondText("ALIVE", ContentType.Text.Plain)
    }
    get("/is_ready") {
        // Let's check if the datasource is still valid and working
        var dbConnectionValid = false
        ds.connection.use { connection ->
            if (connection.isValid(20)) {
                dbConnectionValid = true
            }
        }
        if (!dbConnectionValid) {
            return@get call.respondText("NOT READY", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
        }
        call.respondText("READY", ContentType.Text.Plain)
    }
    get("/metrics") {
        val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()
        call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
            TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
        }
    }
}
