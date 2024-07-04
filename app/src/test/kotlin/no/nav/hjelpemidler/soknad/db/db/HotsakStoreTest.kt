package no.nav.hjelpemidler.soknad.db.db

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.soknad.db.domain.HotsakTilknytningData
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class HotsakStoreTest {
    @Test
    fun `Lag knytning mellom endeleg journalført digital søknad og Hotsak basert på sakId`() = databaseTest {
        val søknadId = UUID.randomUUID() // Digital søknad får denne i kanalreferanseId frå Joark
        val sakId = "1001"

        val hotsakTilknytningData = HotsakTilknytningData(søknadId, sakId)

        testTransaction {
            hotsakStore.lagKnytningMellomSakOgSøknad(hotsakTilknytningData)
            val søknad = hotsakStore.hentVedtaksresultatForSøknad(søknadId).shouldNotBeNull()

            søknad.saksnr shouldBe "1001"
            søknad.vedtaksresultat.shouldBeNull()
            søknad.vedtaksdato.shouldBeNull()
        }
    }

    @Test
    fun `Lagrer vedtaksresultat frå Hotsak`() = databaseTest {
        val søknadId = UUID.randomUUID()
        val sakId = "1002"

        // Før vedtak blir gjort
        val hotsakTilknytningData = HotsakTilknytningData(søknadId, sakId)

        // Etter vedtak er gjort
        val resultat = "I"
        val vedtaksdato = LocalDate.of(2021, 5, 31)

        testTransaction {
            hotsakStore.lagKnytningMellomSakOgSøknad(hotsakTilknytningData)
            hotsakStore.lagreVedtaksresultat(søknadId, resultat, vedtaksdato) shouldBe 1

            val søknad = hotsakStore.hentVedtaksresultatForSøknad(søknadId).shouldNotBeNull()
            søknad.saksnr shouldBe "1002"
            søknad.vedtaksresultat shouldBe "I"
            søknad.vedtaksdato shouldBe vedtaksdato

            hotsakStore.hentSøknadsIdForHotsakNummer("1002") shouldBe søknadId
        }
    }
}
