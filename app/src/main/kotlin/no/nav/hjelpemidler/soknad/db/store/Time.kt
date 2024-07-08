package no.nav.hjelpemidler.soknad.db.store

/**
 * fixme -> bruk prometheus her
 */
inline fun <T : Any?> time(queryName: String, block: () -> T): T = block()
