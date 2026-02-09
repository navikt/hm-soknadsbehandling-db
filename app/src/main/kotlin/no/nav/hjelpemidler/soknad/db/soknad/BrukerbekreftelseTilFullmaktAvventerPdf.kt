package no.nav.hjelpemidler.soknad.db.soknad

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.soknad.db.kafka.Melding
import java.util.UUID

data class BrukerbekreftelseTilFullmaktAvventerPdf(
    override val eventName: String = "hm-brukerbekreftelse-til-fullmakt-avventer-pdf",
    override val eventId: UUID = UUID.randomUUID(),
    val behovsmeldingId: BehovsmeldingId,
) : Melding
