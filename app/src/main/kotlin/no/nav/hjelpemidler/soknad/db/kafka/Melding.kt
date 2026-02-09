package no.nav.hjelpemidler.soknad.db.kafka

import java.util.UUID

interface Melding {
    val eventId: UUID
    val eventName: String
}
