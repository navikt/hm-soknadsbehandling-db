package no.nav.hjelpemidler.soknad.db.rapportering

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.soknad.db.test.testJobb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManglendeBrukerbekreftelseTest {

    @Test
    fun `skal IKKE sende varsel etter 2 dager`() = testJobb {
        lagreBehovsmelding(status = BehovsmeldingStatus.VENTER_GODKJENNING)

        clock.plusDays(2)
        manglendeBrukerbekreftelse.rapporter()

        assertTrue(epostClient.outbox.isEmpty())
    }

    @Test
    fun `skal sende varsel etter 3 dager`() = testJobb {
        lagreBehovsmelding(status = BehovsmeldingStatus.VENTER_GODKJENNING, formidlersEpost = "kari@kommune.no")

        clock.plusDays(3).plusMinutes(1)
        manglendeBrukerbekreftelse.rapporter()

        assertEquals(1, epostClient.outbox.size)
        assertEquals("kari@kommune.no", epostClient.outbox.first().mottaker)
    }

    @Test
    fun `skal varsle flere formidlere`() = testJobb {
        lagreBehovsmelding(status = BehovsmeldingStatus.VENTER_GODKJENNING, formidlersEpost = "kari@kommune.no")
        lagreBehovsmelding(status = BehovsmeldingStatus.VENTER_GODKJENNING, formidlersEpost = "ola@kommune.no")

        clock.plusDays(3).plusMinutes(1)
        manglendeBrukerbekreftelse.rapporter()

        assertEquals(2, epostClient.outbox.size)
        assertTrue(epostClient.outbox.any { it.mottaker == "kari@kommune.no" })
        assertTrue(epostClient.outbox.any { it.mottaker == "ola@kommune.no" })
    }

    @Test
    fun `skal ikke sende flere varsel til samme formidler`() = testJobb {
        lagreBehovsmelding(status = BehovsmeldingStatus.VENTER_GODKJENNING, formidlersEpost = "kari@kommune.no")
        lagreBehovsmelding(status = BehovsmeldingStatus.VENTER_GODKJENNING, formidlersEpost = "kari@kommune.no")

        clock.plusDays(3).plusMinutes(1)
        manglendeBrukerbekreftelse.rapporter()

        assertEquals(1, epostClient.outbox.size)
    }

    @Test
    fun `skal IKKE sende nytt varsel etter 6 dager etter forrige varsling`() = testJobb {
        lagreBehovsmelding(status = BehovsmeldingStatus.VENTER_GODKJENNING, formidlersEpost = "kari@kommune.no")

        clock.plusDays(3).plusMinutes(1)
        manglendeBrukerbekreftelse.rapporter()
        clock.plusDays(6)
        manglendeBrukerbekreftelse.rapporter()

        assertEquals(1, epostClient.outbox.size)
    }

    @Test
    fun `skal sende nytt varsel etter 7 dager etter forrige varsling`() = testJobb {
        lagreBehovsmelding(status = BehovsmeldingStatus.VENTER_GODKJENNING, formidlersEpost = "kari@kommune.no")

        clock.plusDays(3).plusMinutes(1)
        manglendeBrukerbekreftelse.rapporter()
        clock.plusDays(7).plusMinutes(1)
        manglendeBrukerbekreftelse.rapporter()

        assertEquals(2, epostClient.outbox.size)
        assertTrue(epostClient.outbox.all { it.mottaker == "kari@kommune.no" })
    }
}
