package no.nav.hjelpemidler.soknad.db.db

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.migrate
import no.nav.hjelpemidler.database.transactionAsync
import no.nav.hjelpemidler.soknad.mottak.db.InfotrygdStorePostgres
import java.io.Closeable
import javax.sql.DataSource

private val logg = KotlinLogging.logger {}

class Database(private val dataSource: DataSource) : Transaction, Closeable {
    override suspend operator fun <T> invoke(block: StoreProvider.() -> T): T =
        transactionAsync(dataSource, strict = true) {
            StoreProvider(it).block()
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

    class StoreProvider(tx: JdbcOperations) {
        val hotsakStore = HotsakStorePostgres(tx)
        val infotrygdStore = InfotrygdStorePostgres(tx)
        val ordreStore = OrdreStorePostgres(tx)
        val søknadStore = SøknadStorePostgres(tx)
        val søknadStoreInnsender = SøknadStoreInnsenderPostgres(tx)
    }
}
