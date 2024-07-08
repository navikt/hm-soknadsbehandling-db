package no.nav.hjelpemidler.soknad.db.store

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.soknad.db.domain.BehovsmeldingType
import no.nav.hjelpemidler.soknad.db.domain.SoknadData
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.jsonMapper
import no.nav.hjelpemidler.soknad.db.mockSøknad
import no.nav.hjelpemidler.soknad.db.rolle.InnsenderRolle
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadStoreInnsenderTest {
    @Test
    fun `Hent formidlers søknad`() = databaseTest {
        val søknadId = UUID.randomUUID()

        testTransaction {
            søknadStore.save(mockSøknad(søknadId))
            søknadStoreInnsender
                .hentSøknaderForInnsender("12345678910", InnsenderRolle.FORMIDLER)
                .shouldBeSingleton {
                    it.navnBruker shouldBe "Fornavn Etternavn"
                }
        }
    }

    @Test
    fun `Formidler kan kun hente søknad hen selv har sendt inn`() = databaseTest {
        val søknadId = UUID.randomUUID()
        val fnrFormidler = "12345678910"
        val fnrAnnenFormidler = "10987654321"

        testTransaction {
            søknadStore.save(mockSøknad(søknadId, fnrInnsender = fnrFormidler))
            søknadStore.save(mockSøknad(UUID.randomUUID(), fnrInnsender = fnrAnnenFormidler))
            søknadStoreInnsender
                .hentSøknaderForInnsender("12345678910", InnsenderRolle.FORMIDLER) shouldHaveSize 1

            søknadStoreInnsender
                .hentSøknadForInnsender("12345678910", søknadId, InnsenderRolle.FORMIDLER)
                .shouldNotBeNull {
                    this.søknadId shouldBe søknadId
                    this.søknadsdata.shouldNotBeNull()
                }
        }
    }

    @Test
    fun `Navn på bruker fjernes ikke ved sletting`() = databaseTest {
        val søknadId = UUID.randomUUID()

        testTransaction {
            søknadStore.save(mockSøknad(søknadId, Status.VENTER_GODKJENNING)) shouldBe 1
            søknadStore.slettSøknad(søknadId) shouldBe 1

            søknadStoreInnsender
                .hentSøknaderForInnsender("12345678910", InnsenderRolle.FORMIDLER)
                .shouldBeSingleton {
                    it.navnBruker shouldBe "Fornavn Etternavn"
                }
        }
    }

    @Test
    fun `Henter ikke søknad som er 4 uker gammel`() = databaseTest {
        val søknadId = UUID.randomUUID()

        testTransaction { tx ->
            søknadStore.save(
                SoknadData(
                    "15084300133",
                    "Fornavn Etternavn",
                    "12345678910",
                    søknadId,
                    jsonMapper.createObjectNode(),
                    status = Status.SLETTET,
                    kommunenavn = null,
                    er_digital = true,
                    soknadGjelder = null,
                ),
            ) shouldBe 1

            tx.execute("UPDATE V1_SOKNAD SET CREATED = (now() - interval '3 week') WHERE SOKNADS_ID = '$søknadId'")

            søknadStoreInnsender
                .hentSøknaderForInnsender("12345678910", InnsenderRolle.FORMIDLER, 4)
                .shouldHaveSize(1)

            tx.execute("UPDATE V1_SOKNAD SET CREATED = (now() - interval '5 week') WHERE SOKNADS_ID = '$søknadId'")

            søknadStoreInnsender
                .hentSøknaderForInnsender("00102012345", InnsenderRolle.FORMIDLER, 4)
                .shouldBeEmpty()
        }
    }

    @Test
    fun `Fullmakt for søknad for formidler`() = databaseTest {
        val søknadId = UUID.randomUUID()

        testTransaction {
            søknadStore.save(mockSøknad(søknadId, Status.GODKJENT_MED_FULLMAKT)) shouldBe 1
            søknadStore.oppdaterStatus(søknadId, Status.ENDELIG_JOURNALFØRT) shouldBe 1

            søknadStoreInnsender
                .hentSøknaderForInnsender("12345678910", InnsenderRolle.FORMIDLER)
                .shouldBeSingleton {
                    it.fullmakt.shouldBeTrue()
                }
        }
    }

    @Test
    fun `Fullmakt er false hvis bruker skal bekrefte søknaden`() = databaseTest {
        val søknadId = UUID.randomUUID()

        testTransaction {
            søknadStore.save(mockSøknad(søknadId, Status.VENTER_GODKJENNING)) shouldBe 1
            søknadStore.oppdaterStatus(søknadId, Status.GODKJENT) shouldBe 1
            søknadStore.oppdaterStatus(søknadId, Status.ENDELIG_JOURNALFØRT) shouldBe 1

            søknadStoreInnsender
                .hentSøknaderForInnsender("12345678910", InnsenderRolle.FORMIDLER)
                .shouldBeSingleton {
                    it.fullmakt.shouldBeFalse()
                }
        }
    }

    @Test
    fun `Bestiller kan kun hente ut bestillinger`() = databaseTest {
        val idSøknad = UUID.randomUUID()
        val idBestilling = UUID.randomUUID()

        testTransaction {
            søknadStore.save(mockSøknad(idSøknad, behovsmeldingType = BehovsmeldingType.SØKNAD))
            søknadStore.save(mockSøknad(idBestilling, behovsmeldingType = BehovsmeldingType.BESTILLING))

            søknadStoreInnsender
                .hentSøknaderForInnsender("12345678910", InnsenderRolle.BESTILLER)
                .shouldBeSingleton {
                    it.søknadId shouldBe idBestilling
                }

            søknadStoreInnsender
                .hentSøknadForInnsender("12345678910", idBestilling, InnsenderRolle.BESTILLER)
                .shouldNotBeNull()
                .should {
                    it.søknadId shouldBe idBestilling
                    it.behovsmeldingType shouldBe BehovsmeldingType.BESTILLING
                }
        }
    }

    @Test
    fun `Metrikker er non-blocking`() = databaseTest {
        val søknadId = UUID.randomUUID()
        testTransaction {
            søknadStore.save(mockSøknad(søknadId, Status.VENTER_GODKJENNING)) shouldBe 1
            søknadStore.oppdaterStatus(søknadId, Status.GODKJENT) shouldBe 1
            søknadStore.oppdaterStatus(søknadId, Status.VEDTAKSRESULTAT_ANNET) shouldBe 1
        }
    }
}
