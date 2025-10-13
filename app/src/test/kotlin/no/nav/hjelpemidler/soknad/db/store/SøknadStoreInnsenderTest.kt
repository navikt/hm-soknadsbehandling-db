package no.nav.hjelpemidler.soknad.db.store

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.Behovsmeldingsgrunnlag
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import no.nav.hjelpemidler.soknad.db.rolle.InnsenderRolle
import no.nav.hjelpemidler.soknad.db.soknad.lagBehovsmeldingsgrunnlagDigital
import no.nav.hjelpemidler.soknad.db.soknad.lagSøknadId
import org.junit.jupiter.api.Test

class SøknadStoreInnsenderTest {
    @Test
    fun `Hent formidlers søknad`() = databaseTest {
        val søknadId = lagSøknadId()
        val fnrFormidler = lagFødselsnummer()

        testTransaction {
            søknadStore.lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital(søknadId, fnrInnsender = fnrFormidler))
            søknadStoreInnsender
                .hentSøknaderForInnsender(fnrFormidler, InnsenderRolle.FORMIDLER)
                .shouldBeSingleton {
                    it.navnBruker shouldBe "Fornavn Etternavn"
                }
        }
    }

    @Test
    fun `Formidler kan kun hente søknad hen selv har sendt inn`() = databaseTest {
        val søknadId = lagSøknadId()
        val fnrFormidler = lagFødselsnummer()
        val fnrAnnenFormidler = lagFødselsnummer()

        testTransaction {
            søknadStore.lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital(søknadId, fnrInnsender = fnrFormidler))
            søknadStore.lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital(fnrInnsender = fnrAnnenFormidler))
            søknadStoreInnsender
                .hentSøknaderForInnsender(fnrFormidler, InnsenderRolle.FORMIDLER) shouldHaveSize 1

            søknadStoreInnsender
                .hentSøknadForInnsender(fnrFormidler, søknadId, InnsenderRolle.FORMIDLER)
                .shouldNotBeNull {
                    this.søknadId shouldBe søknadId
                    this.behovsmelding.shouldNotBeNull()
                }
        }
    }

    @Test
    fun `Formidler kan hente behovsmeldinger som ligger til godkjenning`() = databaseTest {
        val idTilGodkjenning = lagSøknadId()
        val fnrFormidler = lagFødselsnummer()

        testTransaction {
            søknadStore.lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital(idTilGodkjenning, fnrInnsender = fnrFormidler, status = BehovsmeldingStatus.VENTER_GODKJENNING))
            søknadStore.lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital(fnrInnsender = fnrFormidler, status = BehovsmeldingStatus.GODKJENT_MED_FULLMAKT))

            val tilGodkjenning = søknadStoreInnsender.hentBehovsmeldingerTilGodkjenningForInnsender(fnrFormidler)

            tilGodkjenning shouldHaveSize 1
            tilGodkjenning.first().id shouldBe idTilGodkjenning
        }
    }

    @Test
    fun `Navn på bruker fjernes ikke ved sletting`() = databaseTest {
        val søknadId = lagSøknadId()
        val fnrFormidler = lagFødselsnummer()

        testTransaction {
            søknadStore.lagreBehovsmelding(
                lagBehovsmeldingsgrunnlagDigital(
                    søknadId,
                    BehovsmeldingStatus.VENTER_GODKJENNING,
                    fnrInnsender = fnrFormidler,
                ),
            ) shouldBe 1
            søknadStore.slettSøknad(søknadId) shouldBe 1

            søknadStoreInnsender
                .hentSøknaderForInnsender(fnrFormidler, InnsenderRolle.FORMIDLER)
                .shouldBeSingleton {
                    it.navnBruker shouldBe "Fornavn Etternavn"
                }
        }
    }

    @Test
    fun `Henter ikke søknad som er 4 uker gammel`() = databaseTest {
        val søknadId = lagSøknadId()
        val fnrInnsender = lagFødselsnummer()

        testTransaction { tx ->
            søknadStore.lagreBehovsmelding(
                Behovsmeldingsgrunnlag.Digital(
                    søknadId = søknadId,
                    status = BehovsmeldingStatus.SLETTET,
                    fnrBruker = lagFødselsnummer(),
                    navnBruker = "Fornavn Etternavn",
                    fnrInnsender = fnrInnsender,
                    behovsmelding = emptyMap(),
                    behovsmeldingGjelder = null,
                    behovsmeldingV2 = emptyMap(),
                ),
            ) shouldBe 1

            tx.execute("UPDATE V1_SOKNAD SET CREATED = (now() - interval '3 week') WHERE SOKNADS_ID = '$søknadId'")

            søknadStoreInnsender
                .hentSøknaderForInnsender(fnrInnsender, InnsenderRolle.FORMIDLER, 4)
                .shouldHaveSize(1)

            tx.execute("UPDATE V1_SOKNAD SET CREATED = (now() - interval '5 week') WHERE SOKNADS_ID = '$søknadId'")

            søknadStoreInnsender
                .hentSøknaderForInnsender("00102012345", InnsenderRolle.FORMIDLER, 4)
                .shouldBeEmpty()
        }
    }

    @Test
    fun `Fullmakt for søknad for formidler`() = databaseTest {
        val søknadId = lagSøknadId()
        val fnrFormidler = lagFødselsnummer()

        testTransaction {
            søknadStore.lagreBehovsmelding(
                lagBehovsmeldingsgrunnlagDigital(
                    søknadId,
                    BehovsmeldingStatus.GODKJENT_MED_FULLMAKT,
                    fnrInnsender = fnrFormidler,
                ),
            ) shouldBe 1
            søknadStore.oppdaterStatus(søknadId, BehovsmeldingStatus.ENDELIG_JOURNALFØRT) shouldBe 1

            søknadStoreInnsender
                .hentSøknaderForInnsender(fnrFormidler, InnsenderRolle.FORMIDLER)
                .shouldBeSingleton {
                    it.fullmakt.shouldBeTrue()
                }
        }
    }

    @Test
    fun `Fullmakt er false hvis bruker skal bekrefte søknaden`() = databaseTest {
        val søknadId = lagSøknadId()
        val fnrInnsender = lagFødselsnummer()

        testTransaction {
            søknadStore.lagreBehovsmelding(
                lagBehovsmeldingsgrunnlagDigital(
                    søknadId,
                    BehovsmeldingStatus.VENTER_GODKJENNING,
                    fnrInnsender = fnrInnsender,
                ),
            ) shouldBe 1
            søknadStore.oppdaterStatus(søknadId, BehovsmeldingStatus.GODKJENT) shouldBe 1
            søknadStore.oppdaterStatus(søknadId, BehovsmeldingStatus.ENDELIG_JOURNALFØRT) shouldBe 1

            søknadStoreInnsender
                .hentSøknaderForInnsender(fnrInnsender, InnsenderRolle.FORMIDLER)
                .shouldBeSingleton {
                    it.fullmakt.shouldBeFalse()
                }
        }
    }

    @Test
    fun `Bestiller kan kun hente ut bestillinger`() = databaseTest {
        val idSøknad = lagSøknadId()
        val idBestilling = lagSøknadId()
        val fnrInnsender = lagFødselsnummer()

        testTransaction {
            søknadStore.lagreBehovsmelding(
                lagBehovsmeldingsgrunnlagDigital(
                    idSøknad,
                    fnrInnsender = fnrInnsender,
                    behovsmeldingType = BehovsmeldingType.SØKNAD,
                ),
            )
            søknadStore.lagreBehovsmelding(
                lagBehovsmeldingsgrunnlagDigital(
                    idBestilling,
                    fnrInnsender = fnrInnsender,
                    behovsmeldingType = BehovsmeldingType.BESTILLING,
                ),
            )

            søknadStoreInnsender
                .hentSøknaderForInnsender(fnrInnsender, InnsenderRolle.BESTILLER)
                .shouldBeSingleton {
                    it.søknadId shouldBe idBestilling
                }

            søknadStoreInnsender
                .hentSøknadForInnsender(fnrInnsender, idBestilling, InnsenderRolle.BESTILLER)
                .shouldNotBeNull()
                .should {
                    it.søknadId shouldBe idBestilling
                    it.behovsmeldingType shouldBe BehovsmeldingType.BESTILLING
                }
        }
    }

    @Test
    fun `Metrikker er non-blocking`() = databaseTest {
        val søknadId = lagSøknadId()
        testTransaction {
            søknadStore.lagreBehovsmelding(
                lagBehovsmeldingsgrunnlagDigital(
                    søknadId,
                    BehovsmeldingStatus.VENTER_GODKJENNING,
                ),
            ) shouldBe 1
            søknadStore.oppdaterStatus(søknadId, BehovsmeldingStatus.GODKJENT) shouldBe 1
            søknadStore.oppdaterStatus(søknadId, BehovsmeldingStatus.VEDTAKSRESULTAT_ANNET) shouldBe 1
        }
    }
}
