package no.nav.hjelpemidler.soknad.db.store

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.Testcontainers
import no.nav.hjelpemidler.database.clean
import no.nav.hjelpemidler.database.createDataSource
import no.nav.hjelpemidler.database.migrate
import no.nav.hjelpemidler.database.transaction
import no.nav.hjelpemidler.http.slack.SlackClient
import no.nav.hjelpemidler.http.slack.slack
import no.nav.hjelpemidler.soknad.db.test.MutableClock
import javax.sql.DataSource

val testDataSource: DataSource by lazy {
    val dataSource = createDataSource(Testcontainers) {
        tag = "15-alpine"
    }
    object : DataSource by dataSource {}
}

fun testDatabase(clock: MutableClock = MutableClock()) = TestDatabase(
    dataSource = testDataSource,
    slackClient = slack(MockEngine { respondError(HttpStatusCode.NotImplemented) }),
    clock = clock,
)

fun databaseTest(test: suspend TestDatabase.() -> Unit) = runTest {
    testDatabase().apply {
        clean()
        migrate()
        test()
    }
}

class TestDatabase(
    private val dataSource: DataSource,
    val slackClient: SlackClient,
    val clock: MutableClock,
) : Transaction by Database(
    dataSource = dataSource,
    slackClient = slackClient,
    clock = clock,
) {
    suspend fun clean(): Unit = withContext(Dispatchers.IO) {
        dataSource.clean {
            cleanDisabled(false)
        }
    }

    suspend fun migrate(): Unit = withContext(Dispatchers.IO) {
        dataSource.migrate()
    }

    suspend fun <T> testTransaction(block: Database.StoreProvider.(JdbcOperations) -> T): T = transaction(dataSource, strict = true) {
        Database.StoreProvider(it, slackClient, clock).block(it)
    }
}
