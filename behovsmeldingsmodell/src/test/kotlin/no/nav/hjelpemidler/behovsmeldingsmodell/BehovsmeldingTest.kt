package no.nav.hjelpemidler.behovsmeldingsmodell

import no.nav.hjelpemidler.behovsmeldingsmodell.test.jsonMapper
import no.nav.hjelpemidler.behovsmeldingsmodell.test.readResource
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.BehovsmeldingResponse
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test

class BehovsmeldingTest {
    @Test
    fun `Skal kunne deserialisere både gammel og ny JSON til felles behovsmeldingsmodell`() {
        assertDoesNotThrow {
            jsonMapper.readResource<BehovsmeldingResponse>("/behovsmeldinger/behovsmelding_søknad_eldre.json")
        }

        assertDoesNotThrow {
            jsonMapper.readResource<BehovsmeldingResponse>("/behovsmeldinger/behovsmelding_søknad_nyere.json")
        }

        assertDoesNotThrow {
            jsonMapper.readResource<BehovsmeldingResponse>("/behovsmeldinger/behovsmelding_bestilling.json")
        }

        assertDoesNotThrow {
            jsonMapper.readResource<BehovsmeldingResponse>("/behovsmeldinger/behovsmelding_bytte.json")
        }

        assertDoesNotThrow {
            jsonMapper.readResource<BehovsmeldingResponse>("/behovsmeldinger/behovsmelding_brukerpassbytte.json")
        }
    }
}
