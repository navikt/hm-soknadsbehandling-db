package no.nav.hjelpemidler.soknad.db.store

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.Testcontainers
import no.nav.hjelpemidler.database.createDataSource
import no.nav.hjelpemidler.database.createRole
import no.nav.hjelpemidler.database.migrate
import no.nav.hjelpemidler.database.transactionAsync
import no.nav.hjelpemidler.database.withDatabaseContext
import javax.sql.DataSource

val testDatabase by lazy {
    TestDatabase(
        createDataSource(Testcontainers) {
            tag = "15-alpine"
        },
    )
}

fun databaseTest(test: suspend TestDatabase.() -> Unit) = runTest {
    testDatabase.migrate()
    testDatabase.test()
}

class TestDatabase(private val dataSource: DataSource) : Transaction by Database(dataSource) {
    suspend fun migrate(): Unit = withDatabaseContext {
        dataSource.migrate { createRole("cloudsqliamuser") }
    }

    suspend fun <T> testTransaction(block: Database.StoreProvider.(JdbcOperations) -> T): T = transactionAsync(dataSource, strict = true) {
        Database.StoreProvider(it, mockk(relaxed = true)).block(it)
    }
}
