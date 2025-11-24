package no.nav.hjelpemidler.soknad.db.rapportering.epost

import com.fasterxml.jackson.annotation.JsonValue

data class SendMailRequest(
    val message: Message,
    val saveToSentItems: Boolean = true,
)

data class Message(
    val subject: String,
    val body: ItemBody,
    val toRecipients: List<Recipient>,
)

data class ItemBody(
    val contentType: ContentType,
    val content: String,
)

enum class ContentType(@get:JsonValue val value: String) {
    TEXT("Text"),
    HTML("HTML"),
}

data class Recipient(
    val emailAddress: EmailAddress,
)

data class EmailAddress(
    val address: String,
)
