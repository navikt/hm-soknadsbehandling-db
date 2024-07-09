package no.nav.hjelpemidler.soknad.db.store

import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.soknad.db.domain.lagOrdrelinje
import org.junit.jupiter.api.Test

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
