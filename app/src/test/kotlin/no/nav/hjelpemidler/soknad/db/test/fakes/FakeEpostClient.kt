package no.nav.hjelpemidler.soknad.db.test.fakes

import no.nav.hjelpemidler.soknad.db.rapportering.epost.ContentType
import no.nav.hjelpemidler.soknad.db.rapportering.epost.EpostClient

class FakeEpostClient : EpostClient {

    val outbox = mutableListOf<FakeEpost>()

    override suspend fun send(
        avsender: String,
        mottaker: String,
        tittel: String,
        innhold: String,
        innholdstype: ContentType,
        lagreIUtboks: Boolean,
    ) {
        outbox.add(
            FakeEpost(
                avsender = avsender,
                mottaker = mottaker,
                tittel = tittel,
                innhold = innhold,
                innholdstype = innholdstype,
                lagreIUtboks = lagreIUtboks,
            ),
        )
    }
}

data class FakeEpost(
    val avsender: String,
    val mottaker: String,
    val tittel: String,
    val innhold: String,
    val innholdstype: ContentType,
    val lagreIUtboks: Boolean,
)
