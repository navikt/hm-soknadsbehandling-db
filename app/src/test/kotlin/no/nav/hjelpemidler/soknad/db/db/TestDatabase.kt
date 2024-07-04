package no.nav.hjelpemidler.soknad.db.db

import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.Testcontainers
import no.nav.hjelpemidler.database.createDataSource
import no.nav.hjelpemidler.database.flyway
import no.nav.hjelpemidler.database.transaction

private val dataSource by lazy {
    createDataSource(Testcontainers) {
        tag = "15-alpine"
    }.apply {
        transaction(this) {
            it.execute("DROP ROLE IF EXISTS cloudsqliamuser")
            it.execute("CREATE ROLE cloudsqliamuser")
        }
    }
}

private val flyway by lazy {
    dataSource.flyway {
        cleanDisabled(false)
    }
}

fun databaseTest(test: () -> Unit) {
    flyway.clean()
    flyway.migrate()
    test()
}

fun <T> testTransaction(block: Database.StoreProvider.(JdbcOperations) -> T): T {
    return transaction(dataSource, strict = true) {
        Database.StoreProvider(it).block(it)
    }
}
