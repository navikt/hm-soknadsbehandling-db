package no.nav.hjelpemidler.soknad.db.store

import no.nav.hjelpemidler.soknad.db.store.Database.StoreProvider

interface Transaction {
    suspend operator fun <T> invoke(returnGeneratedKeys: Boolean = false, block: suspend StoreProvider.() -> T): T
}
