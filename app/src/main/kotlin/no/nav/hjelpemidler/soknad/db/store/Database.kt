package no.nav.hjelpemidler.soknad.db.store

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.engine.apache.Apache
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.migrate
import no.nav.hjelpemidler.database.transactionAsync
import no.nav.hjelpemidler.http.slack.SlackClient
import no.nav.hjelpemidler.http.slack.slack
import java.io.Closeable
import java.time.Clock
import javax.sql.DataSource

private val logg = KotlinLogging.logger {}

class Database(private val dataSource: DataSource, private val clock: Clock) :
    Transaction,
    Closeable {
    private val slackClient = slack(engine = Apache.create())

    override suspend operator fun <T> invoke(returnGeneratedKeys: Boolean, block: suspend StoreProvider.() -> T): T = transactionAsync(dataSource, strict = true, returnGeneratedKeys = returnGeneratedKeys) {
        StoreProvider(it, slackClient, clock).block()
    }

    fun migrate() {
        logg.info { "Migrerer databasen..." }
        dataSource.migrate()
    }

    override fun close() {
        if (dataSource is Closeable) {
            logg.info { "Stopper databasen..." }
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
