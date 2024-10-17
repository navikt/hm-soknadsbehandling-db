package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import no.nav.hjelpemidler.behovsmeldingsmodell.test.jsonMapper
import no.nav.hjelpemidler.behovsmeldingsmodell.test.readResource
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Behovsmelding
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.BehovsmeldingResponse
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.mapping.tilInnsenderbehovsmeldingV2
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertTrue

class InnsenderbehovsmeldingMapperTest {

    private val søknad =
        jsonMapper.readResource<BehovsmeldingResponse>("/behovsmeldinger/behovsmelding_søknad_nyere.json")
            .behovsmelding.søknad!!

    @Test
    fun `Skal feile ved ikke-godkjent HTML i en opplysning sin ledetekst`() {
        assertThrows<IllegalArgumentException> {
            Opplysning(
                ledetekst = LokalisertTekst("<script>alert('XSS');</script>"),
                innhold = "ok",
            )
        }
    }

    @Test
    fun `Skal feile ved ikke-godkjent HTML i en opplysning sitt innhold`() {
        assertDoesNotThrow {
            Opplysning(
                ledetekst = LokalisertTekst("ok"),
                innhold = "<a href=\"javascript:alert('XSS')\">Vi bryr oss ikke om HTML i fritekst</a>",
            )
        }

        assertThrows<IllegalArgumentException> {
            Opplysning(
                ledetekst = LokalisertTekst("ok"),
                innhold = LokalisertTekst("<a href=\"javascript:alert('XSS')\">Click me</a>"),
            )
        }
    }

    @Test
    fun `Skal ikke feile ved ikke-godkjent HTML i brukernavn`() {
        val fornavn = "<div title='John's profile'>Bryr oss ikke om HTML her</div>"
        val behovsmeldingV1 =
            Behovsmelding(søknad = søknad.copy(bruker = søknad.bruker.copy(fornavn = fornavn)))
        val behovsmeldingV2 = assertDoesNotThrow { tilInnsenderbehovsmeldingV2(behovsmeldingV1) }
        assertTrue(behovsmeldingV2.brukersituasjon.vilkår.any { it.tekst.nb.contains(fornavn) })
    }
}
