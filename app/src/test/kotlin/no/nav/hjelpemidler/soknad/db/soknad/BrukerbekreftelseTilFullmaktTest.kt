package no.nav.hjelpemidler.soknad.db.soknad

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.Signaturtype
import no.nav.hjelpemidler.domain.person.toFødselsnummer
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import no.nav.hjelpemidler.soknad.db.exception.BehovsmeldingNotFoundException
import no.nav.hjelpemidler.soknad.db.exception.BehovsmeldingUgyldigStatusException
import no.nav.hjelpemidler.soknad.db.test.testJobb
import kotlin.test.Test

class BrukerbekreftelseTilFullmaktTest {
    @Test
    fun `Skal konvertere behovsmelding fra brukerbekreftelse til fullmakt`() = testJobb {
        val grunnlag = lagreBehovsmelding(status = BehovsmeldingStatus.VENTER_GODKJENNING, signaturtype = Signaturtype.BRUKER_BEKREFTER)
        val behovsmeldingId = grunnlag.søknadId
        val innsenderFnr = grunnlag.fnrInnsender.toFødselsnummer()!!

        søknadService.konverterBrukerbekreftelseToFullmakt(behovsmeldingId, innsenderFnr)

        transaction {
            val behovsmeldingEtter = søknadStore.finnInnsenderbehovsmelding(behovsmeldingId)!!
            behovsmeldingEtter.bruker.signaturtype shouldBe Signaturtype.FULLMAKT

            val statusEtter = søknadStore.hentStatus(behovsmeldingId)
            statusEtter shouldBe BehovsmeldingStatus.FULLMAKT_AVVENTER_PDF
        }
    }

    @Test
    fun `Skal kaste exception når behovsmelding ikke finnes`() = testJobb {
        val behovsmeldingId = lagSøknadId()
        val innsenderFnr = lagFødselsnummer().toFødselsnummer()

        shouldThrow<BehovsmeldingNotFoundException> {
            søknadService.konverterBrukerbekreftelseToFullmakt(behovsmeldingId, innsenderFnr)
        }
    }

    @Test
    fun `Skal kaste exception når behovsmelding har feil status`() = testJobb {
        val grunnlag = lagreBehovsmelding(status = BehovsmeldingStatus.INNSENDT_FULLMAKT_IKKE_PÅKREVD)
        val behovsmeldingId = grunnlag.søknadId
        val innsenderFnr = grunnlag.fnrInnsender.toFødselsnummer()!!

        shouldThrow<BehovsmeldingUgyldigStatusException> {
            søknadService.konverterBrukerbekreftelseToFullmakt(behovsmeldingId, innsenderFnr)
        }
    }

    @Test
    fun `Skal beholde alle andre felter når signaturtype endres`() = testJobb {
        val grunnlag = lagreBehovsmelding(status = BehovsmeldingStatus.VENTER_GODKJENNING, signaturtype = Signaturtype.BRUKER_BEKREFTER)
        val behovsmeldingId = grunnlag.søknadId
        val innsenderFnr = grunnlag.fnrInnsender.toFødselsnummer()!!

        val originalBehovsmelding = transaction {
            søknadStore.finnInnsenderbehovsmelding(behovsmeldingId)!!
        }

        søknadService.konverterBrukerbekreftelseToFullmakt(behovsmeldingId, innsenderFnr)

        val oppdatertBehovsmelding = transaction {
            søknadStore.finnInnsenderbehovsmelding(behovsmeldingId)!!
        }

        // Verifiser at kun signaturtype er endret
        oppdatertBehovsmelding.id shouldBe originalBehovsmelding.id
        oppdatertBehovsmelding.bruker.fnr shouldBe originalBehovsmelding.bruker.fnr
        oppdatertBehovsmelding.bruker.navn shouldBe originalBehovsmelding.bruker.navn
        oppdatertBehovsmelding.levering shouldBe originalBehovsmelding.levering
        oppdatertBehovsmelding.hjelpemidler shouldBe originalBehovsmelding.hjelpemidler

        // Og at signaturtype er endret
        oppdatertBehovsmelding.bruker.signaturtype shouldBe Signaturtype.FULLMAKT
        originalBehovsmelding.bruker.signaturtype shouldBe Signaturtype.BRUKER_BEKREFTER
    }
}
