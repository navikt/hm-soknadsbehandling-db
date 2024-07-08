package no.nav.hjelpemidler.soknad.db.store

import no.nav.hjelpemidler.soknad.db.store.Database.StoreProvider

interface Transaction {
    suspend operator fun <T> invoke(block: StoreProvider.() -> T): T
}
