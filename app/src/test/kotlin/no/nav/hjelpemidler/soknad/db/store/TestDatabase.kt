package no.nav.hjelpemidler.soknad.db.store

import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.Testcontainers
import no.nav.hjelpemidler.database.clean
import no.nav.hjelpemidler.database.createDataSource
import no.nav.hjelpemidler.database.createRole
import no.nav.hjelpemidler.database.migrate
import no.nav.hjelpemidler.database.transactionAsync
import no.nav.hjelpemidler.soknad.db.test.MutableClock
import java.util.UUID
import javax.sql.DataSource

val testDataSource by lazy {
    createDataSource(Testcontainers) {
        tag = "15-alpine"
    }
}

fun testDatabase(clock: MutableClock = MutableClock()) = TestDatabase(
    testDataSource,
    clock,
)

fun databaseTest(test: suspend TestDatabase.() -> Unit) = runTest {
    testDatabase().apply {
        clean()
        migrate()
        test()
    }
}

class TestDatabase(private val dataSource: DataSource, val clock: MutableClock) : Transaction by Database(dataSource, clock) {

    suspend fun clean(): Unit = withContext(Dispatchers.IO) {
        dataSource.clean {
            cleanDisabled(false)
        }
    }

    suspend fun migrate(): Unit = withContext(Dispatchers.IO) {
        dataSource.migrate { createRole("cloudsqliamuser") }
    }

    suspend fun <T> testTransaction(block: Database.StoreProvider.(JdbcOperations) -> T): T = transactionAsync(dataSource, strict = true) {
        Database.StoreProvider(it, mockk(relaxed = true), clock).block(it)
    }
}
