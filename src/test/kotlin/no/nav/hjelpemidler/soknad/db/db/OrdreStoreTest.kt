package no.nav.hjelpemidler.soknad.db.db

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.soknad.db.domain.OrdrelinjeData
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OrdreStoreTest {

    @Test
    fun `Lagr ordrelinje frå OEBS`() {
        val ordrelinje = OrdrelinjeData(
            søknadId = UUID.randomUUID(),
            fnrBruker = "15084300133",
            serviceforespørsel = 19162211,
            ordrenr = 6181503,
            ordrelinje = 1,
            delordrelinje = 1,
            artikkelnr = "123456",
            antall = 1.0,
            produktgruppe = "Manuelle armdrevne rullestoler",
            data = ObjectMapper().readTree(""" {"key": "value"} """),
        )
        withMigratedDb {
            OrdreStorePostgres(DataSource.instance).apply {
                this.save(ordrelinje).also { it shouldBe 1 }
            }
        }
    }

    @Test
    fun `Forsøk på lagring av identiske ordrelinjer frå OEBS gir ingen endringar for duplikatet`() {
        val ordrelinje = OrdrelinjeData(
            søknadId = UUID.randomUUID(),
            fnrBruker = "15084300133",
            serviceforespørsel = 19162211,
            ordrenr = 6181503,
            ordrelinje = 1,
            delordrelinje = 1,
            artikkelnr = "123456",
            antall = 1.0,
            produktgruppe = "Manuelle armdrevne rullestoler",
            data = ObjectMapper().readTree(""" {"key": "value"} """),
        )
        withMigratedDb {
            OrdreStorePostgres(DataSource.instance).apply {
                this.save(ordrelinje).also { it shouldBe 1 }
                this.save(ordrelinje).also { it shouldBe 0 }
            }
        }
    }
}
