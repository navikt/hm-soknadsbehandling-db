package no.nav.hjelpemidler.soknad.db.rapportering.epost

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.configuration.Environment

private val log = KotlinLogging.logger {}

interface EpostClient {
    suspend fun send(
        avsender: String,
        mottaker: String,
        tittel: String,
        innhold: String,
        innholdstype: ContentType,
        lagreIUtboks: Boolean = false,
    )
}

class GraphEpost(
    val client: GraphClient,
) : EpostClient {

    override suspend fun send(
        avsender: String,
        mottaker: String,
        tittel: String,
        innhold: String,
        innholdstype: ContentType,
        lagreIUtboks: Boolean,
    ) {
        val request = SendMailRequest(
            message = Message(
                subject = tittel,
                body = ItemBody(innholdstype, innhold),
                toRecipients = listOf(Recipient(EmailAddress(mottaker))),
            ),
            saveToSentItems = lagreIUtboks,
        )

        if (!Environment.current.isProd) {
            log.info { "Hopper over sending av epost for ikke-prod. Avsender=$avsender, epostrequest=$request" }
            return
        }

        client.sendEpost(request, avsender)
    }
}

const val EPOST_DIGIHOT = "digitalisering.av.hjelpemidler.og.tilrettelegging@nav.no"
