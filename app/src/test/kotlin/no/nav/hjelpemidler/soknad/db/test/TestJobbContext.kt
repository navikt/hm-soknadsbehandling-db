package no.nav.hjelpemidler.soknad.db.test

import kotlinx.coroutines.test.runTest
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.Behovsmeldingsgrunnlag
import no.nav.hjelpemidler.behovsmeldingsmodell.Signaturtype
import no.nav.hjelpemidler.soknad.db.kafka.LocalKafkaClient
import no.nav.hjelpemidler.soknad.db.rapportering.ManglendeBrukerbekreftelse
import no.nav.hjelpemidler.soknad.db.soknad.SøknadService
import no.nav.hjelpemidler.soknad.db.soknad.lagBehovsmeldingsgrunnlagDigital
import no.nav.hjelpemidler.soknad.db.store.Transaction
import no.nav.hjelpemidler.soknad.db.store.testDatabase
import no.nav.hjelpemidler.soknad.db.test.fakes.FakeEpostClient

class TestJobbContext(
    val transaction: Transaction,
    val clock: MutableClock,
    val epostClient: FakeEpostClient = FakeEpostClient(),
    val manglendeBrukerbekreftelse: ManglendeBrukerbekreftelse = ManglendeBrukerbekreftelse(
        transaction,
        epostClient,
        clock,
    ),
    val søknadService: SøknadService = SøknadService(transaction, LocalKafkaClient),
) {

    suspend inline fun lagreBehovsmelding(
        status: BehovsmeldingStatus = BehovsmeldingStatus.VENTER_GODKJENNING,
        formidlersEpost: String = "formidler@kommune.no",
        signaturtype: Signaturtype = Signaturtype.FULLMAKT,
    ): Behovsmeldingsgrunnlag.Digital {
        val grunnlag = lagBehovsmeldingsgrunnlagDigital(
            status = status,
            formidlersEpost = formidlersEpost,
            signaturtype = signaturtype,
        )
        søknadService.lagreBehovsmelding(grunnlag)
        return grunnlag
    }
}

fun testJobb(test: suspend TestJobbContext.() -> Unit) = runTest {
    val clock = MutableClock()
    val database = testDatabase(clock).apply {
        clean()
        migrate()
    }
    val testContext = TestJobbContext(database, clock)
    testContext.test()
}
