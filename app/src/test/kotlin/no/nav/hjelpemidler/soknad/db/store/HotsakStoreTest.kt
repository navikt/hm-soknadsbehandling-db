package no.nav.hjelpemidler.soknad.db.store

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSakId
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class HotsakStoreTest {
    @Test
    fun `Lag knytning mellom endelig journalført digital søknad og Hotsak basert på sakId`() = databaseTest {
        val søknadId = UUID.randomUUID() // Digital søknad får denne i kanalReferanseId fra Joark
        val sakId = HotsakSakId("1001")

        testTransaction {
            hotsakStore.lagKnytningMellomSakOgSøknad(søknadId, sakId)
            val sak = hotsakStore.finnSak(søknadId).shouldNotBeNull()

            sak.sakId shouldBe sakId
            sak.vedtak.shouldBeNull()
        }
    }

    @Test
    fun `Lagrer vedtaksresultat fra Hotsak`() = databaseTest {
        val søknadId = UUID.randomUUID()
        val sakId = HotsakSakId("1002")

        // Etter vedtak er gjort
        val vedtaksresultat = "I"
        val vedtaksdato = LocalDate.of(2021, 5, 31)

        testTransaction {
            hotsakStore.lagKnytningMellomSakOgSøknad(søknadId, sakId)
            hotsakStore.lagreVedtaksresultat(søknadId, vedtaksresultat, vedtaksdato) shouldBe 1

            val sak = hotsakStore.finnSak(søknadId).shouldNotBeNull()
            sak.sakId shouldBe sakId
            val vedtak = sak.vedtak.shouldNotBeNull()
            vedtak.vedtaksresultat shouldBe vedtaksresultat
            vedtak.vedtaksdato shouldBe vedtaksdato

            hotsakStore.finnSak(sak.sakId)?.søknadId shouldBe søknadId
        }
    }
}
