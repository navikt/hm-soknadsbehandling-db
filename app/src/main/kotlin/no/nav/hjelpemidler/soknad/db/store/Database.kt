package no.nav.hjelpemidler.soknad.db.store

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.TransactionContext
import no.nav.hjelpemidler.database.migrate
import no.nav.hjelpemidler.database.transaction
import no.nav.hjelpemidler.http.slack.SlackClient
import java.io.Closeable
import java.time.Clock
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

class Database(
    private val dataSource: DataSource,
    private val slackClient: SlackClient,
    private val clock: Clock,
) : Transaction,
    AutoCloseable {
    override suspend operator fun <T> invoke(
        returnGeneratedKeys: Boolean,
        block: suspend context(TransactionContext) StoreProvider.() -> T,
    ): T = transaction(dataSource, strict = true, returnGeneratedKeys = returnGeneratedKeys) {
        StoreProvider(it, slackClient, clock).block()
    }

    fun migrate() {
        log.info { "Migrerer databasen" }
        try {
            dataSource.migrate()
        } catch (e: Exception) {
            log.error(e) { "Migrering av databasen feilet" }
            throw e
        }
    }

    override fun close() {
        if (Environment.current.isTest) return
        if (dataSource is Closeable) {
            log.info { "Stopper databasen" }
            dataSource.close()
        }
    }

    class StoreProvider(tx: JdbcOperations, slackClient: SlackClient, clock: Clock) {
        val hotsakStore = HotsakStore(tx)
        val infotrygdStore = InfotrygdStore(tx)
        val ordreStore = OrdreStore(tx)
        val søknadStore = SøknadStore(tx, slackClient, clock)
        val søknadStoreInnsender = SøknadStoreInnsender(tx)
        val brukerbekreftelseVarselStore = BrukerbekreftelseVarselStore(tx, clock)
    }
}
