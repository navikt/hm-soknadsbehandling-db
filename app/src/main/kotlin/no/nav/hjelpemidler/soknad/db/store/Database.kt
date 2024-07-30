package no.nav.hjelpemidler.soknad.db.store

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.engine.apache.Apache
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.migrate
import no.nav.hjelpemidler.database.transactionAsync
import no.nav.hjelpemidler.http.slack.SlackClient
import no.nav.hjelpemidler.http.slack.slack
import java.io.Closeable
import javax.sql.DataSource

private val logg = KotlinLogging.logger {}

class Database(private val dataSource: DataSource) : Transaction, Closeable {
    private val slackClient = slack(engine = Apache.create())

    override suspend operator fun <T> invoke(block: StoreProvider.() -> T): T =
        transactionAsync(dataSource, strict = true) {
            StoreProvider(it, slackClient).block()
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

    class StoreProvider(tx: JdbcOperations, slackClient: SlackClient) {
        val hotsakStore = HotsakStore(tx)
        val infotrygdStore = InfotrygdStore(tx)
        val ordreStore = OrdreStore(tx)
        val søknadStore = SøknadStore(tx, slackClient)
        val søknadStoreInnsender = SøknadStoreInnsender(tx)
    }
}
