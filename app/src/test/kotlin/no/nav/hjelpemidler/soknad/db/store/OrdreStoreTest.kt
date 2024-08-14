package no.nav.hjelpemidler.soknad.db.store

import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.soknad.db.ordre.lagOrdrelinje
import org.junit.jupiter.api.Test

class OrdreStoreTest {
    @Test
    fun `Lagre ordrelinje fra OEBS`() = databaseTest {
        val ordrelinje = lagOrdrelinje()

        testTransaction {
            ordreStore.lagre(ordrelinje.søknadId, ordrelinje) shouldBe 1
        }
    }

    @Test
    fun `Forsøk på lagring av identiske ordrelinjer fra OEBS gir ingen endringer for duplikatet`() = databaseTest {
        val ordrelinje = lagOrdrelinje()

        testTransaction {
            ordreStore.lagre(ordrelinje.søknadId, ordrelinje) shouldBe 1
            ordreStore.lagre(ordrelinje.søknadId, ordrelinje) shouldBe 0
        }
    }
}
