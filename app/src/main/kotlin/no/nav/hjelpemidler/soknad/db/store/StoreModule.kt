package no.nav.hjelpemidler.soknad.db.store

import io.ktor.server.plugins.di.DependencyRegistry
import io.ktor.server.plugins.di.provide
import no.nav.hjelpemidler.database.PostgreSQL
import no.nav.hjelpemidler.database.createDataSource
import javax.sql.DataSource

fun DependencyRegistry.dataSource() {
    provide<DataSource> {
        createDataSource(PostgreSQL) {
            envVarPrefix = "DB"
        }
    }
}

fun DependencyRegistry.database() {
    provide(::Database)
}
