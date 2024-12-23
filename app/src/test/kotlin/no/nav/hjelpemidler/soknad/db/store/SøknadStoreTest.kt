package no.nav.hjelpemidler.soknad.db.store

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.Behovsmeldingsgrunnlag
import no.nav.hjelpemidler.behovsmeldingsmodell.Funksjonsnedsettelser
import no.nav.hjelpemidler.behovsmeldingsmodell.LeveringTilleggsinfo
import no.nav.hjelpemidler.domain.geografi.Veiadresse
import no.nav.hjelpemidler.domain.person.Personnavn
import no.nav.hjelpemidler.serialization.jackson.jsonToValue
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import no.nav.hjelpemidler.soknad.db.soknad.lagBehovsmeldingsgrunnlagDigital
import no.nav.hjelpemidler.soknad.db.soknad.lagBehovsmeldingsgrunnlagPapir
import no.nav.hjelpemidler.soknad.db.soknad.lagSøknadId
import no.nav.hjelpemidler.soknad.db.soknad.mockSøknadMedRullestol
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SøknadStoreTest {
    @Test
    fun `Hent lagret søknad 1`() = databaseTest {
        val søknadId = lagSøknadId()
        val fnrBruker = lagFødselsnummer()

        testTransaction {
            søknadStore.lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital(søknadId, fnrBruker = fnrBruker))
            val søknad = søknadStore.hentSøknad(søknadId)

            assertEquals(fnrBruker, søknad?.innsenderbehovsmelding!!.bruker.fnr.value)
            assertEquals("Fornavn", søknad.innsenderbehovsmelding.bruker.navn.fornavn)
            assertEquals("Etternavn", søknad.innsenderbehovsmelding.bruker.navn.etternavn)
            assertEquals("12345678", søknad.innsenderbehovsmelding.bruker.telefon)
            assertEquals("adresseveien 2", søknad.innsenderbehovsmelding.bruker.veiadresse?.adresse)
            assertEquals("1234", søknad.innsenderbehovsmelding.bruker.veiadresse?.postnummer)
            assertEquals("poststed", søknad.innsenderbehovsmelding.bruker.veiadresse?.poststed)
            assertEquals(
                Personnavn("formidlerFornavn", etternavn = "formidlerEtternavn"),
                søknad.innsenderbehovsmelding.levering.hjelpemiddelformidler.navn,
            )
            assertEquals("arbeidssted", søknad.innsenderbehovsmelding.levering.hjelpemiddelformidler.arbeidssted)
            assertEquals("stilling", søknad.innsenderbehovsmelding.levering.hjelpemiddelformidler.stilling)
            assertEquals(
                Veiadresse("postadresse arbeidssted", "9999", "poststed"),
                søknad.innsenderbehovsmelding.levering.hjelpemiddelformidler.adresse,
            )
            assertEquals("12345678", søknad.innsenderbehovsmelding.levering.hjelpemiddelformidler.telefon)
            assertEquals("treffesEnklest", søknad.innsenderbehovsmelding.levering.hjelpemiddelformidler.treffesEnklest)
            assertEquals("formidler@kommune.no", søknad.innsenderbehovsmelding.levering.hjelpemiddelformidler.epost)
            assertNull(søknad.innsenderbehovsmelding.levering.annenOppfølgingsansvarlig)
            assertTrue(søknad.innsenderbehovsmelding.bruker.legacyopplysninger.any { it.ledetekst.nb == "Boform" && it.innhold.nb == "Hjemme" })
            assertTrue(søknad.innsenderbehovsmelding.bruker.legacyopplysninger.any { it.ledetekst.nb == "Bruksarena" && it.innhold.nb == "Dagliglivet" })
            assertEquals(
                setOf(Funksjonsnedsettelser.BEVEGELSE, Funksjonsnedsettelser.HØRSEL),
                søknad.innsenderbehovsmelding.brukersituasjon.funksjonsnedsettelser,
            )

            assertEquals(2, søknad.innsenderbehovsmelding.hjelpemidler.totaltAntall)
            assertEquals(1, søknad.innsenderbehovsmelding.hjelpemidler.hjelpemidler.size)
            with(søknad.innsenderbehovsmelding.hjelpemidler.hjelpemidler.first()) {
                assertEquals(1, antall)
                assertEquals("Hjelpemiddelnavn", produkt.artikkelnavn)
                assertEquals("Arbeidsstoler", produkt.sortimentkategori)
                assertEquals("123456", produkt.hmsArtNr)
                assertTrue(opplysninger.any { it.ledetekst.nb == "Kommentar" && it.innhold.first().fritekst == "Tilleggsinformasjon" })
                assertEquals(1, produkt.rangering)
                assertEquals(true, utlevertinfo.alleredeUtlevertFraHjelpemiddelsentralen)
                assertTrue(opplysninger.any { it.ledetekst.nb == "Behov" && it.innhold.first().fritekst == "Tilleggsinfo" })
                assertEquals(1, tilbehør.size)
                assertEquals("654321", tilbehør.first().hmsArtNr)
                assertEquals("Tilbehør 1", tilbehør.first().navn)
                assertEquals(1, tilbehør.first().antall)
                assertTrue(opplysninger.any { it.ledetekst.nb == "Kan ikke ha tilsvarende fordi" && it.innhold.first().fritekst == "begrunnelse" })
            }

            assertNull(søknad.innsenderbehovsmelding.levering.annenUtleveringsadresse)

            assertEquals(true, søknad?.er_digital)
        }
    }

    @Test
    fun `Hent lagret søknad 2`() = databaseTest {
        val søknadId = UUID.randomUUID()
        val fnrBruker = lagFødselsnummer()
        val fnrInnsender = lagFødselsnummer()

        testTransaction {
            søknadStore.lagreBehovsmelding(
                Behovsmeldingsgrunnlag.Digital(
                    fnrBruker = fnrBruker,
                    navnBruker = "Fornavn Etternavn",
                    fnrInnsender = fnrInnsender,
                    søknadId = søknadId,
                    behovsmelding = jsonToValue(
                        """
                                {
                                  "fnrBruker": "$fnrBruker",
                                  "soknadId": "62f68547-11ae-418c-8ab7-4d2af985bcd9",
                                  "datoOpprettet": "2021-02-23T09:46:45.146+00:00",
                                  "soknad": {
                                    "id": "e8dac11d-fa66-4561-89d7-88a62ab31c2b",
                                    "date": "2021-02-16",
                                    "bruker": {
                                      "kilde": "PDL",
                                      "adresse": "Trandemveien 29",
                                      "fnummer": "$fnrBruker",
                                      "fornavn": "Sedat",
                                      "poststed": "Hebnes",
                                      "signatur": "BRUKER_BEKREFTER",
                                      "etternavn": "Kronjuvel",
                                      "postnummer": "4235",
                                      "telefonNummer": "12341234"
                                    },
                                    "levering": {
                                      "hmfEpost": "anders@andersen.no",
                                      "hmfPostnr": "1212",
                                      "hmfFornavn": "Sedat",
                                      "hmfTelefon": "12121212",
                                      "opfFornavn": "",
                                      "opfTelefon": "",
                                      "hmfPoststed": "Oslo",
                                      "hmfStilling": "Ergo",
                                      "opfStilling": "",
                                      "hmfEtternavn": "Kronjuvel",
                                      "opfAnsvarFor": "",
                                      "opfEtternavn": "",
                                      "hmfArbeidssted": "Oslo",
                                      "hmfPostadresse": "Oslovegen",
                                      "opfArbeidssted": "",
                                      "opfRadioButton": "Hjelpemiddelformidler",
                                      "utleveringPostnr": "",
                                      "hmfTreffesEnklest": "Måndag",
                                      "utleveringFornavn": "",
                                      "utleveringTelefon": "",
                                      "utleveringPoststed": "",
                                      "utleveringEtternavn": "",
                                      "merknadTilUtlevering": "",
                                      "utleveringPostadresse": "",
                                      "utleveringsmaateRadioButton": null,
                                      "utlevertInfo": {
                                        "utlevertType": "Overført",
                                        "overførtFraBruker": "1234"
                                      },
                                      "tilleggsinfo": [
                                        "UTLEVERING_KALENDERAPP"
                                      ]
                                    },
                                    "hjelpemidler": {
                                      "hjelpemiddelListe": [
                                        {
                                          "navn": "Topro Skråbrett",
                                          "hmsNr": "243544",
                                          "antall": 1,
                                          "produkt": {
                                            "artid": "108385",
                                            "artno": "815061",
                                            "newsid": "4289",
                                            "prodid": "30389",
                                            "apostid": "860",
                                            "apostnr": "3",
                                            "artname": "Topro Skråbrett",
                                            "isocode": "18301505",
                                            "stockid": "243544",
                                            "isotitle": "Terskeleliminatorer",
                                            "kategori": "Terskeleliminatorer og ramper",
                                            "postrank": "1",
                                            "prodname": "Topro Skråbrett",
                                            "artpostid": "14309",
                                            "adescshort": "Bredde 90 cm. Lengde 77 cm.",
                                            "aposttitle": "Post 3: Terskeleleminator - påkjøring fra en side. Velegnet for utendørs bruk",
                                            "pshortdesc": "Skråbrett i aluminium utførelse med sklisikker overflate. Leveres som standard i bredder fra 90 - 126 cm og justerbar høyde fra 5 - 20 cm.",
                                            "cleanposttitle": "Terskeleleminator - påkjøring fra en side. Velegnet for utendørs bruk",
                                            "techdataAsText": "Påkjøring forfra JA, Bredde 90cm, Lengde maks 77cm, Terskelhøyde min 5cm, Terskelhøyde maks 20cm, Vekt 8kg, Belastning maks 350kg, Fastmontert JA, Festemåte fastmontert, Materiale aluminium, Sklisikker overflate JA",
                                            "cleanTechdataAsText": " Bredde 90cm,  Lengde maks 77cm,  Terskelhøyde min 5cm,  Terskelhøyde maks 20cm"
                                          },
                                          "uniqueKey": "2435441613472031819",
                                          "beskrivelse": "Topro Skråbrett",
                                          "begrunnelsen": "",
                                          "tilbehorListe": [],
                                          "vilkaroverskrift": "",
                                          "kanIkkeTilsvarande": "false",
                                          "tilleggsinformasjon": "",
                                          "hjelpemiddelkategori": "Terskeleliminatorer og ramper",
                                          "utlevertFraHjelpemiddelsentralen": false
                                        }
                                      ],
                                      "hjelpemiddelTotaltAntall": 1
                                    },
                                    "brukersituasjon": {
                                      "storreBehov": true,
                                      "nedsattFunksjon": true,
                                      "praktiskeProblem": true,
                                      "bostedRadioButton": "Hjemme",
                                      "nedsattFunksjonTypes": {
                                        "horsel": false,
                                        "bevegelse": true,
                                        "kognisjon": false
                                      },
                                      "bruksarenaErDagliglivet": true
                                    }
                                  }
                                }
                        """.trimIndent(),
                    ),
                    status = BehovsmeldingStatus.VENTER_GODKJENNING,
                    behovsmeldingGjelder = null,
                ),
            )
            val hentSoknad = søknadStore.hentSøknad(søknadId)
            assertEquals(fnrBruker, hentSoknad!!.innsenderbehovsmelding!!.bruker.fnr.value)
            assertEquals(true, hentSoknad.er_digital)
            assertTrue(
                hentSoknad.innsenderbehovsmelding!!.levering.automatiskUtledetTilleggsinfo.contains(
                    LeveringTilleggsinfo.UTLEVERING_KALENDERAPP,
                ),
            )
        }
    }

    @Test
    fun `Lagre søknad`() = databaseTest {
        testTransaction {
            søknadStore.lagreBehovsmelding(
                Behovsmeldingsgrunnlag.Digital(
                    søknadId = lagSøknadId(),
                    status = BehovsmeldingStatus.VENTER_GODKJENNING,
                    fnrBruker = lagFødselsnummer(),
                    navnBruker = "Fornavn Etternavn",
                    fnrInnsender = lagFødselsnummer(),
                    behovsmelding = emptyMap(),
                    behovsmeldingGjelder = null,
                ),
            ) shouldBe 1
        }
    }

    @Test
    fun `Søknaden blir ikke oppdatert til samme status igjen`() = databaseTest {
        val søknadId = lagSøknadId()
        val status1 = BehovsmeldingStatus.VENTER_GODKJENNING
        testTransaction {
            søknadStore.lagreBehovsmelding(
                lagBehovsmeldingsgrunnlagDigital(
                    søknadId,
                    status1,
                ),
            )
        } shouldBe 1

        val status2 = BehovsmeldingStatus.GODKJENT
        testTransaction { søknadStore.oppdaterStatus(søknadId, status2) } shouldBe 1
        testTransaction { søknadStore.finnSøknad(søknadId) }.shouldNotBeNull { status shouldBe status2 }
        testTransaction { søknadStore.oppdaterStatus(søknadId, status2) } shouldBe 0
        testTransaction { søknadStore.finnSøknad(søknadId) }.shouldNotBeNull { status shouldBe status2 }

        val status3 = BehovsmeldingStatus.ENDELIG_JOURNALFØRT
        testTransaction { søknadStore.oppdaterStatus(søknadId, status3) } shouldBe 1
        testTransaction { søknadStore.finnSøknad(søknadId) }.shouldNotBeNull { status shouldBe status3 }
    }

    @Test
    fun `Fullmakt for søknad innsendt av formidler`() = databaseTest {
        val søknadId = lagSøknadId()
        val fnrBruker = lagFødselsnummer()

        testTransaction {
            søknadStore.lagreBehovsmelding(
                lagBehovsmeldingsgrunnlagDigital(
                    søknadId,
                    BehovsmeldingStatus.GODKJENT_MED_FULLMAKT,
                    fnrBruker,
                ),
            ) shouldBe 1
        }
        testTransaction {
            søknadStore.oppdaterStatus(søknadId, BehovsmeldingStatus.ENDELIG_JOURNALFØRT) shouldBe 1
        }
        testTransaction {
            søknadStore.hentSøknaderForBruker(fnrBruker).shouldBeSingleton {
                it.fullmakt.shouldBeTrue()
            }
        }
    }

    @Test
    fun `Ikke fullmakt for søknad med brukers godkjenning`() = databaseTest {
        val søknadId = lagSøknadId()
        val fnrBruker = lagFødselsnummer()

        testTransaction {
            søknadStore.lagreBehovsmelding(
                lagBehovsmeldingsgrunnlagDigital(
                    søknadId,
                    BehovsmeldingStatus.VENTER_GODKJENNING,
                    fnrBruker,
                ),
            ) shouldBe 1
        }
        testTransaction {
            søknadStore.oppdaterStatus(søknadId, BehovsmeldingStatus.GODKJENT) shouldBe 1
        }
        testTransaction {
            søknadStore.oppdaterStatus(søknadId, BehovsmeldingStatus.ENDELIG_JOURNALFØRT) shouldBe 1
        }
        testTransaction {
            søknadStore.hentSøknaderForBruker(fnrBruker).shouldBeSingleton {
                it.fullmakt.shouldBeFalse()
            }
        }
    }

    @Test
    fun `Søknad er utgått`() = databaseTest {
        val søknadId = lagSøknadId()
        val fnrBruker = lagFødselsnummer()

        testTransaction { tx ->
            søknadStore.lagreBehovsmelding(
                Behovsmeldingsgrunnlag.Digital(
                    søknadId = søknadId,
                    status = BehovsmeldingStatus.VENTER_GODKJENNING,
                    fnrBruker = fnrBruker,
                    navnBruker = "Fornavn Etternavn",
                    fnrInnsender = lagFødselsnummer(),
                    behovsmelding = emptyMap(),
                    behovsmeldingGjelder = null,
                ),
            ) shouldBe 1
            tx.execute("UPDATE V1_SOKNAD SET CREATED = (now() - interval '2 week') WHERE SOKNADS_ID = '$søknadId'")
        }

        testTransaction {
            søknadStore.hentSøknaderTilGodkjenningEldreEnn(14).shouldBeSingleton()
        }

        testTransaction { tx ->
            tx.execute("UPDATE V1_SOKNAD SET CREATED = (now() - interval '13 day') WHERE SOKNADS_ID = '$søknadId'")
        }

        testTransaction {
            søknadStore.hentSøknaderTilGodkjenningEldreEnn(14).shouldBeEmpty()
        }
    }

    @Test
    fun `Hent godkjente søknader uten oppgave`() = databaseTest {
        val søknadId1 = lagSøknadId()
        val søknadId2 = lagSøknadId()

        testTransaction { tx ->
            søknadStore.lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital(søknadId1, BehovsmeldingStatus.GODKJENT))
            søknadStore.lagreBehovsmelding(
                lagBehovsmeldingsgrunnlagDigital(
                    søknadId2,
                    BehovsmeldingStatus.GODKJENT_MED_FULLMAKT,
                ),
            )
        }
        testTransaction { tx ->
            tx.execute("UPDATE V1_SOKNAD SET CREATED = (now() - interval '2 day') WHERE SOKNADS_ID = '$søknadId1'")
        }
        testTransaction {
            søknadStore.hentGodkjenteBehovsmeldingerUtenOppgaveEldreEnn(2) shouldHaveSize 1
        }
        testTransaction { tx ->
            tx.execute("UPDATE V1_SOKNAD SET CREATED = (now() - interval '3 day') WHERE SOKNADS_ID = '$søknadId2'")
        }
        testTransaction {
            søknadStore.hentGodkjenteBehovsmeldingerUtenOppgaveEldreEnn(2) shouldHaveSize 2
        }
        testTransaction { tx ->
            tx.execute("UPDATE V1_SOKNAD SET CREATED = (now() - interval '1 day') WHERE SOKNADS_ID = '$søknadId1'")
        }
        testTransaction {
            søknadStore.hentGodkjenteBehovsmeldingerUtenOppgaveEldreEnn(2) shouldHaveSize 1
        }
        testTransaction { tx ->
            tx.execute("UPDATE V1_SOKNAD SET oppgaveid = '123456' WHERE SOKNADS_ID = '$søknadId2'")
        }
        testTransaction {
            søknadStore.hentGodkjenteBehovsmeldingerUtenOppgaveEldreEnn(2).shouldBeEmpty()
        }
    }

    @Test
    fun `Oppdater oppgaveId og ikke overskriv eksisterende oppgaveId`() = databaseTest {
        val søknadId = lagSøknadId()
        val oppgaveId = "102030"

        testTransaction {
            søknadStore.lagreBehovsmelding(
                Behovsmeldingsgrunnlag.Digital(
                    søknadId = søknadId,
                    status = BehovsmeldingStatus.GODKJENT_MED_FULLMAKT,
                    fnrBruker = lagFødselsnummer(),
                    navnBruker = "Fornavn Etternavn",
                    fnrInnsender = lagFødselsnummer(),
                    behovsmelding = emptyMap(),
                    behovsmeldingGjelder = null,
                ),
            )

            søknadStore.oppdaterOppgaveId(søknadId, oppgaveId) shouldBe 1
            søknadStore.oppdaterOppgaveId(søknadId, oppgaveId) shouldBe 0
        }
    }

    @Test
    fun `Papirsøknad lagres i databasen`() = databaseTest {
        val søknadId = lagSøknadId()

        testTransaction {
            søknadStore.lagrePapirsøknad(
                Behovsmeldingsgrunnlag.Papir(
                    søknadId = søknadId,
                    status = BehovsmeldingStatus.ENDELIG_JOURNALFØRT,
                    fnrBruker = lagFødselsnummer(),
                    navnBruker = "Fornavn Etternavn",
                    journalpostId = "1020",
                    sakstilknytning = null,
                ),
            ) shouldBe 1
        }
    }

    @Test
    fun `Papirsøknad lagres ikke som digital søknad`() = databaseTest {
        val søknadId = lagSøknadId()
        testTransaction {
            søknadStore.lagrePapirsøknad(lagBehovsmeldingsgrunnlagPapir(søknadId))
            val søknad = søknadStore.hentSøknad(søknadId)
            assertEquals(false, søknad?.er_digital)
        }
    }

    @Test
    fun `Rullestolinfo blir hentet ut`() = databaseTest {
        val søknadId = lagSøknadId()

        testTransaction {
            søknadStore.lagreBehovsmelding(mockSøknadMedRullestol(søknadId))
            val søknad = søknadStore.hentSøknad(søknadId)
            val hjelpemiddel = søknad!!.innsenderbehovsmelding!!.hjelpemidler.hjelpemidler.first()

            assertEquals(5, hjelpemiddel.opplysninger.size)
            assertTrue(
                hjelpemiddel.opplysninger.any {
                    it.ledetekst.nb == "Bil" &&
                        it.innhold.first().forhåndsdefinertTekst!!.nb == "Rullestolen skal brukes som sete i bil"
                },
            )

            assertTrue(
                hjelpemiddel.opplysninger.any {
                    it.ledetekst.nb == "Sittepute" &&
                        it.innhold.first().forhåndsdefinertTekst!!.nb == "Bruker skal ha sittepute"
                },
            )
        }
    }

    @Test
    fun `Kroppsmål vises i riktig rekkefølge`() = databaseTest {
        /**
         * Det er viktig at kroppsmål vises i riktig rekkefølge: setebredde -> lårlengde -> legglengde,
         * fordi dette sitter på automatikk i fingrene til saksbehandlerene. Dersom vi plutselig endrer
         * på rekkefølgen så kan de fort taste feil uten å tenke over dette.
         */
        val søknadId = lagSøknadId()
        testTransaction {
            søknadStore.lagreBehovsmelding(mockSøknadMedRullestol(søknadId))
            val søknad = søknadStore.hentSøknad(søknadId)
            val hjelpemiddel = søknad!!.innsenderbehovsmelding!!.hjelpemidler.hjelpemidler.first()

            assertTrue(
                hjelpemiddel.opplysninger.any {
                    it.ledetekst.nb == "Kroppsmål" &&
                        it.innhold.first().forhåndsdefinertTekst!!.nb == "Setebredde: 23 cm, lårlengde: 56 cm, legglengde: 23 cm, høyde: 176 cm, kroppsvekt: 99 kg."
                },
            )
        }
    }
}
