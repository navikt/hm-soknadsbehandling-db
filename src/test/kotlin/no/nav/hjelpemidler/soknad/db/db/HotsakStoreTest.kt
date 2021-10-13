package no.nav.hjelpemidler.soknad.db.db

import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.soknad.db.domain.HotsakTilknytningData
import no.nav.hjelpemidler.soknad.db.domain.VedtaksresultatHotsakData
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class HotsakStoreTest {

    @Test
    fun `Lag knytning mellom endeleg journalført digital søknad og Hotsak basert på sakId`() {

        val søknadId = UUID.randomUUID() // Digital søknad får denne i kanalreferanseId frå Joark
        val fnrBruker = "15084300133"
        val sakId = "1001"

        val hotsakTilknytningData = HotsakTilknytningData(
            søknadId,
            saksnr = sakId
        )

        withMigratedDb {
            HotsakStorePostgres(DataSource.instance).apply {
                this.lagKnytningMellomSakOgSøknad(hotsakTilknytningData)
                val søknad: VedtaksresultatHotsakData? = this.hentVedtaksresultatForSøknad(søknadId)
                assertEquals("1001", søknad?.saksnr)
                assertNull(søknad?.vedtaksresultat)
                assertNull(søknad?.vedtaksdato)
            }
        }
    }

    @Test
    fun `Lagrer vedtaksresultat frå Hotsak`() {
        val søknadId = UUID.randomUUID()
        val sakId = "1002"

        // Før vedtak blir gjort
        val hotsakTilknytningData = HotsakTilknytningData(
            søknadId,
            saksnr = sakId
        )

        // Etter vedtak er gjort
        val resultat = "I"
        val vedtaksdato = LocalDate.of(2021, 5, 31)

        withMigratedDb {
            HotsakStorePostgres(DataSource.instance).apply {
                this.lagKnytningMellomSakOgSøknad(hotsakTilknytningData)
            }
            HotsakStorePostgres(DataSource.instance).apply {
                this.lagreVedtaksresultat(søknadId, resultat, vedtaksdato)
                    .also {
                        it shouldBe (1)
                    }
            }
            HotsakStorePostgres(DataSource.instance).apply {
                val søknad = this.hentVedtaksresultatForSøknad(søknadId)
                assertEquals("1002", søknad?.saksnr)
                assertEquals("I", søknad?.vedtaksresultat)
                assertEquals(LocalDate.of(2021, 5, 31).toString(), søknad?.vedtaksdato.toString())
            }
        }
    }
}
