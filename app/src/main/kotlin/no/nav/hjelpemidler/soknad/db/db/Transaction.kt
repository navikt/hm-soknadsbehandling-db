package no.nav.hjelpemidler.soknad.db.db

import no.nav.hjelpemidler.soknad.db.db.Database.StoreProvider

interface Transaction {
    suspend operator fun <T> invoke(block: StoreProvider.() -> T): T
}
