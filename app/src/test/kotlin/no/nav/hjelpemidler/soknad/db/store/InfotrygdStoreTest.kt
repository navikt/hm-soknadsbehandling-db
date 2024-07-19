package no.nav.hjelpemidler.soknad.db.store

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import no.nav.hjelpemidler.soknad.db.domain.lagSøknadId
import no.nav.hjelpemidler.soknad.db.sak.InfotrygdSakId
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InfotrygdStoreTest {
    @Test
    fun `Lag knytning mellom endeleg journalført digital søknad og Infotrygd basert på fagsakId`() = databaseTest {
        val søknadId = lagSøknadId() // Digital søknad får denne i kanalreferanseId frå Joark
        val fnrBruker = lagFødselsnummer()
        val fagsakId = InfotrygdSakId("4703C13")

        testTransaction {
            infotrygdStore.lagKnytningMellomSakOgSøknad(søknadId, fagsakId, fnrBruker)
            val søknad = infotrygdStore.finnSak(søknadId).shouldNotBeNull()
            assertEquals(fnrBruker, søknad.fnrBruker)
            assertEquals("4703", søknad.trygdekontornummer)
            assertEquals("C", søknad.saksblokk)
            assertEquals("13", søknad.saksnummer)
            assertNull(søknad.vedtak)
        }
    }

    @Test
    fun `Skal lagre vedtaksresultat fra Infotrygd`() = databaseTest {
        val søknadId = UUID.randomUUID()
        val fnrBruker = lagFødselsnummer()
        val fagsakId = InfotrygdSakId("4703C13")

        // Etter vedtak er gjort
        val resultat = "IM"
        val vedtaksdato = LocalDate.of(2021, 5, 31)

        testTransaction {
            infotrygdStore.lagKnytningMellomSakOgSøknad(søknadId, fagsakId, fnrBruker)
            infotrygdStore
                .lagreVedtaksresultat(søknadId, resultat, vedtaksdato, "")
                .also { it shouldBe (1) }

            val sak = infotrygdStore.finnSak(søknadId).shouldNotBeNull()
            assertEquals(fnrBruker, sak.fnrBruker)
            assertEquals("4703", sak.trygdekontornummer)
            assertEquals("C", sak.saksblokk)
            assertEquals("13", sak.saksnummer)
            assertEquals("IM", sak.vedtak?.vedtaksresultat)
            assertEquals(LocalDate.of(2021, 5, 31), sak.vedtak?.vedtaksdato)
        }
    }

    @Test
    fun `Hent søknadId frå resultat`() = databaseTest {
        val søknadId = UUID.fromString("62f68547-11ae-418c-8ab7-4d2af985bcd9")
        val fnrBruker = lagFødselsnummer()
        val fagsakId = InfotrygdSakId("4703C13")

        val resultat = "IM"
        val vedtaksdato = LocalDate.of(2021, 5, 31)

        testTransaction {
            infotrygdStore.lagKnytningMellomSakOgSøknad(søknadId, fagsakId, fnrBruker)
            infotrygdStore
                .lagreVedtaksresultat(søknadId, resultat, vedtaksdato, "")
                .also { it shouldBe (1) }

            val søknadIdResultat =
                infotrygdStore.hentSøknadIdFraVedtaksresultatV1(fnrBruker, "C13", LocalDate.of(2021, 5, 31))
            assertEquals("62f68547-11ae-418c-8ab7-4d2af985bcd9", søknadIdResultat.toString())
        }
    }

    /**
     * Fleire enn eitt treff gjer det umogleg å matche OEBS-data mot éin søknad.
     */
    @Test
    fun `Hent søknadId frå resultat skal returnere null viss det ikkje er nøyaktig eitt treff`() = databaseTest {
        val fnrBruker = lagFødselsnummer()
        val søknadId1 = lagSøknadId()
        val fagsakId1 = InfotrygdSakId("4703C14")

        val søknadId2 = lagSøknadId()
        val fagsakId2 = InfotrygdSakId("4719C14")

        val resultat = "IM"
        val vedtaksdato = LocalDate.of(2021, 5, 31)

        testTransaction {
            infotrygdStore.lagKnytningMellomSakOgSøknad(søknadId1, fagsakId1, fnrBruker)
            infotrygdStore.lagKnytningMellomSakOgSøknad(søknadId2, fagsakId2, fnrBruker)

            infotrygdStore
                .lagreVedtaksresultat(søknadId1, resultat, vedtaksdato, "")
                .also { it shouldBe (1) }
            infotrygdStore
                .lagreVedtaksresultat(søknadId2, resultat, vedtaksdato, "")
                .also { it shouldBe (1) }

            val alteredLines =
                infotrygdStore.hentSøknadIdFraVedtaksresultatV1(fnrBruker, "C14", LocalDate.of(2021, 5, 31))
            assertEquals(null, alteredLines)
        }
    }
}
