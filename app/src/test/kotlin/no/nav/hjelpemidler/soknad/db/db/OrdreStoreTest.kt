package no.nav.hjelpemidler.soknad.db.db

import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.soknad.db.domain.OrdrelinjeData
import no.nav.hjelpemidler.soknad.db.jsonMapper
import org.junit.jupiter.api.Test
import java.util.UUID

class OrdreStoreTest {
    @Test
    fun `Lagre ordrelinje fra OEBS`() = databaseTest {
        val ordrelinje = lagOrdrelinje()

        testTransaction {
            ordreStore.save(ordrelinje) shouldBe 1
        }
    }

    @Test
    fun `Forsøk på lagring av identiske ordrelinjer fra OEBS gir ingen endringer for duplikatet`() = databaseTest {
        val ordrelinje = lagOrdrelinje()

        testTransaction {
            ordreStore.save(ordrelinje) shouldBe 1
            ordreStore.save(ordrelinje) shouldBe 0
        }
    }
}

private fun lagOrdrelinje(): OrdrelinjeData = OrdrelinjeData(
    søknadId = UUID.randomUUID(),
    oebsId = 123,
    fnrBruker = "15084300133",
    serviceforespørsel = 19162211,
    ordrenr = 6181503,
    ordrelinje = 1,
    delordrelinje = 1,
    artikkelnr = "123456",
    antall = 1.0,
    enhet = "STK",
    produktgruppe = "Manuelle armdrevne rullestoler",
    produktgruppeNr = "012345",
    hjelpemiddeltype = "Hjelpemiddel",
    data = jsonMapper.createObjectNode(),
)
