package no.nav.hjelpemidler.soknad.db.db

/**
 * fixme -> bruk prometheus her
 */
inline fun <T : Any?> time(queryName: String, block: () -> T): T = block()
