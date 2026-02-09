package no.nav.hjelpemidler.soknad.db.soknad

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.soknad.db.kafka.Melding
import java.util.UUID

data class BrukerbekreftelseTilFullmaktMelding(
    override val eventName: String = "hm-brukerbekreftelse-endret-til-fullmakt",
    override val eventId: UUID = UUID.randomUUID(),
    val behovsmeldingId: BehovsmeldingId,
) : Melding
