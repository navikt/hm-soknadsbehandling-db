package no.nav.hjelpemidler.soknad.db.store

import kotlinx.coroutines.test.runTest
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.Testcontainers
import no.nav.hjelpemidler.database.createDataSource
import no.nav.hjelpemidler.database.flyway
import no.nav.hjelpemidler.database.transaction
import no.nav.hjelpemidler.database.transactionAsync
import no.nav.hjelpemidler.database.withDatabaseContext
import org.flywaydb.core.Flyway
import javax.sql.DataSource

val testDatabase by lazy {
    TestDatabase(
        createDataSource(Testcontainers) {
            tag = "15-alpine"
        }.apply {
            transaction(this) {
                it.execute("DROP ROLE IF EXISTS cloudsqliamuser")
                it.execute("CREATE ROLE cloudsqliamuser")
            }
        },
    )
}

fun databaseTest(test: suspend TestDatabase.() -> Unit) = runTest {
    // Testene kjører mye fortere uten clean(), men testene må da passe på å lage unike testdata
    // testDatabase.clean()
    testDatabase.migrate()
    testDatabase.test()
}

class TestDatabase(private val dataSource: DataSource) : Transaction by Database(dataSource) {
    private val flyway: Flyway = dataSource.flyway { cleanDisabled(false) }
    suspend fun clean(): Unit = withDatabaseContext { flyway.clean() }
    suspend fun migrate(): Unit = withDatabaseContext { flyway.migrate() }

    suspend fun <T> testTransaction(block: Database.StoreProvider.(JdbcOperations) -> T): T =
        transactionAsync(dataSource, strict = true) {
            Database.StoreProvider(it).block(it)
        }
}
