package no.nav.hjelpemidler.soknad.db.store

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.soknad.db.domain.BruksarenaBruker
import no.nav.hjelpemidler.soknad.db.domain.Funksjonsnedsettelse
import no.nav.hjelpemidler.soknad.db.domain.LeveringTilleggsinfo
import no.nav.hjelpemidler.soknad.db.domain.PapirSøknadData
import no.nav.hjelpemidler.soknad.db.domain.SitteputeValg
import no.nav.hjelpemidler.soknad.db.domain.SoknadData
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import no.nav.hjelpemidler.soknad.db.domain.lagSøknadId
import no.nav.hjelpemidler.soknad.db.jsonMapper
import no.nav.hjelpemidler.soknad.db.mockSøknad
import no.nav.hjelpemidler.soknad.db.mockSøknadMedRullestol
import no.nav.hjelpemidler.soknad.db.test.readTree
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SøknadStoreTest {
    @Test
    fun `Hent lagret søknad 1`() = databaseTest {
        val søknadId = lagSøknadId()
        val fnrBruker = lagFødselsnummer()

        testTransaction {
            søknadStore.save(mockSøknad(søknadId, fnrBruker = fnrBruker))
            val søknad = søknadStore.hentSoknad(søknadId)

            assertEquals(fnrBruker, søknad?.søknadsdata?.bruker?.fnummer)
            assertEquals("fornavn", søknad?.søknadsdata?.bruker?.fornavn)
            assertEquals("etternavn", søknad?.søknadsdata?.bruker?.etternavn)
            assertEquals("12345678", søknad?.søknadsdata?.bruker?.telefonNummer)
            assertNull(søknad?.søknadsdata?.bruker?.adresse)
            assertNull(søknad?.søknadsdata?.bruker?.postnummer)
            assertEquals("poststed", søknad?.søknadsdata?.bruker?.poststed)
            assertEquals("formidlerFornavn formidlerEtternavn", søknad?.søknadsdata?.formidler?.navn)
            assertEquals("arbeidssted", søknad?.søknadsdata?.formidler?.arbeidssted)
            assertEquals("stilling", søknad?.søknadsdata?.formidler?.stilling)
            assertEquals("postadresse arbeidssted 9999 poststed", søknad?.søknadsdata?.formidler?.adresse)
            assertEquals("12345678", søknad?.søknadsdata?.formidler?.telefon)
            assertEquals("treffesEnklest", søknad?.søknadsdata?.formidler?.treffesEnklest)
            assertEquals("formidler@kommune.no", søknad?.søknadsdata?.formidler?.epost)
            assertNull(søknad?.søknadsdata?.oppfolgingsansvarlig)
            assertEquals("Hjemme", søknad?.søknadsdata?.bruker?.boform)
            assertEquals(BruksarenaBruker.DAGLIGLIVET, søknad?.søknadsdata?.bruker?.bruksarena)
            assertEquals(
                listOf(Funksjonsnedsettelse.BEVEGELSE, Funksjonsnedsettelse.HØRSEL),
                søknad?.søknadsdata?.bruker?.funksjonsnedsettelser,
            )

            assertEquals(2, søknad?.søknadsdata?.hjelpemiddelTotalAntall)
            assertEquals(1, søknad?.søknadsdata?.hjelpemidler?.size)
            assertEquals(1, søknad?.søknadsdata?.hjelpemidler?.first()?.antall)
            assertEquals("Hjelpemiddelnavn", søknad?.søknadsdata?.hjelpemidler?.first()?.navn)
            assertEquals("beskrivelse", søknad?.søknadsdata?.hjelpemidler?.first()?.beskrivelse)
            assertEquals("Arbeidsstoler", søknad?.søknadsdata?.hjelpemidler?.first()?.hjelpemiddelkategori)
            assertEquals("123456", søknad?.søknadsdata?.hjelpemidler?.first()?.hmsNr)
            assertEquals("Tilleggsinformasjon", søknad?.søknadsdata?.hjelpemidler?.first()?.tilleggsinformasjon)
            assertEquals("1", søknad?.søknadsdata?.hjelpemidler?.first()?.rangering)
            assertEquals(true, søknad?.søknadsdata?.hjelpemidler?.first()?.utlevertFraHjelpemiddelsentralen)
            assertEquals(1, søknad?.søknadsdata?.hjelpemidler?.first()?.vilkarliste?.size)
            assertEquals(
                "Vilkår 1",
                søknad?.søknadsdata?.hjelpemidler?.first()?.vilkarliste?.first()?.vilkaarTekst,
            )
            assertEquals(
                "Tilleggsinfo",
                søknad?.søknadsdata?.hjelpemidler?.first()?.vilkarliste?.first()?.tilleggsinfo,
            )
            assertEquals(1, søknad?.søknadsdata?.hjelpemidler?.first()?.tilbehorListe?.size)
            assertEquals("654321", søknad?.søknadsdata?.hjelpemidler?.first()?.tilbehorListe?.first()?.hmsnr)
            assertEquals("Tilbehør 1", søknad?.søknadsdata?.hjelpemidler?.first()?.tilbehorListe?.first()?.navn)
            assertEquals(1, søknad?.søknadsdata?.hjelpemidler?.first()?.tilbehorListe?.first()?.antall)
            assertEquals("begrunnelse", søknad?.søknadsdata?.hjelpemidler?.first()?.begrunnelse)
            assertEquals(true, søknad?.søknadsdata?.hjelpemidler?.first()?.kanIkkeTilsvarande)

            assertNull(søknad?.søknadsdata?.levering?.adresse)

            assertEquals(true, søknad?.er_digital)
        }
    }

    @Test
    fun `Hent lagret søknad 2`() = databaseTest {
        val søknadId = UUID.randomUUID()
        val fnrBruker = lagFødselsnummer()
        val fnrInnsender = lagFødselsnummer()

        testTransaction {
            søknadStore.save(
                SoknadData(
                    fnrBruker = fnrBruker,
                    navnBruker = "Fornavn Etternavn",
                    fnrInnsender = fnrInnsender,
                    soknadId = søknadId,
                    soknad = readTree(
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
                    status = Status.VENTER_GODKJENNING,
                    kommunenavn = null,
                    er_digital = true,
                    soknadGjelder = null,
                ),
            )
            val hentSoknad = søknadStore.hentSoknad(søknadId)
            assertEquals(fnrBruker, hentSoknad!!.søknadsdata!!.bruker.fnummer)
            assertEquals(true, hentSoknad.er_digital)
            assertEquals(
                LeveringTilleggsinfo.UTLEVERING_KALENDERAPP,
                hentSoknad.søknadsdata!!.levering.tilleggsinfo.first(),
            )
        }
    }

    @Test
    fun `Lagre søknad`() = databaseTest {
        testTransaction {
            søknadStore.save(
                SoknadData(
                    fnrBruker = lagFødselsnummer(),
                    navnBruker = "Fornavn Etternavn",
                    fnrInnsender = lagFødselsnummer(),
                    soknadId = UUID.randomUUID(),
                    soknad = jsonMapper.createObjectNode(),
                    status = Status.VENTER_GODKJENNING,
                    kommunenavn = null,
                    er_digital = true,
                    soknadGjelder = null,
                ),
            ) shouldBe 1
        }
    }

    @Test
    fun `Fullmakt for søknad innsendt av formidler`() = databaseTest {
        val søknadId = UUID.randomUUID()
        val fnrBruker = lagFødselsnummer()

        testTransaction {
            søknadStore.save(mockSøknad(søknadId, Status.GODKJENT_MED_FULLMAKT, fnrBruker)) shouldBe 1
        }
        testTransaction {
            søknadStore.oppdaterStatus(søknadId, Status.ENDELIG_JOURNALFØRT) shouldBe 1
        }
        testTransaction {
            søknadStore.hentSoknaderForBruker(fnrBruker).shouldBeSingleton {
                it.fullmakt.shouldBeTrue()
            }
        }
    }

    @Test
    fun `Ikke fullmakt for søknad med brukers godkjenning`() = databaseTest {
        val søknadId = UUID.randomUUID()
        val fnrBruker = lagFødselsnummer()

        testTransaction {
            søknadStore.save(mockSøknad(søknadId, Status.VENTER_GODKJENNING, fnrBruker)) shouldBe 1
        }
        testTransaction {
            søknadStore.oppdaterStatus(søknadId, Status.GODKJENT) shouldBe 1
        }
        testTransaction {
            søknadStore.oppdaterStatus(søknadId, Status.ENDELIG_JOURNALFØRT) shouldBe 1
        }
        testTransaction {
            søknadStore.hentSoknaderForBruker(fnrBruker).shouldBeSingleton {
                it.fullmakt.shouldBeFalse()
            }
        }
    }

    @Test
    fun `Søknad er utgått`() = databaseTest {
        val søknadId = UUID.randomUUID()
        val fnrBruker = lagFødselsnummer()

        testTransaction { tx ->
            søknadStore.save(
                SoknadData(
                    fnrBruker = fnrBruker,
                    navnBruker = "Fornavn Etternavn",
                    fnrInnsender = lagFødselsnummer(),
                    soknadId = søknadId,
                    soknad = jsonMapper.createObjectNode(),
                    status = Status.VENTER_GODKJENNING,
                    kommunenavn = null,
                    er_digital = true,
                    soknadGjelder = null,
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
        val søknadId1 = UUID.randomUUID()
        val søknadId2 = UUID.randomUUID()

        testTransaction { tx ->
            søknadStore.save(mockSøknad(søknadId1, Status.GODKJENT))
            søknadStore.save(mockSøknad(søknadId2, Status.GODKJENT_MED_FULLMAKT))
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
        val søknadId = UUID.randomUUID()
        val oppgaveId = "102030"

        testTransaction {
            søknadStore.save(
                SoknadData(
                    fnrBruker = lagFødselsnummer(),
                    navnBruker = "Fornavn Etternavn",
                    fnrInnsender = lagFødselsnummer(),
                    soknadId = søknadId,
                    soknad = jsonMapper.createObjectNode(),
                    status = Status.GODKJENT_MED_FULLMAKT,
                    kommunenavn = null,
                    er_digital = true,
                    soknadGjelder = null,
                ),
            )

            søknadStore.oppdaterOppgaveId(søknadId, oppgaveId) shouldBe 1
            søknadStore.oppdaterOppgaveId(søknadId, oppgaveId) shouldBe 0
        }
    }

    @Test
    fun `Papirsøknad lagres i databasen`() = databaseTest {
        val søknadId = UUID.randomUUID()

        testTransaction {
            søknadStore.savePapir(
                PapirSøknadData(
                    fnrBruker = lagFødselsnummer(),
                    soknadId = søknadId,
                    status = Status.ENDELIG_JOURNALFØRT,
                    journalpostid = 1020,
                    navnBruker = "Fornavn Etternavn",
                ),
            ) shouldBe 1
        }
    }

    @Test
    fun `Papirsøknad lagres ikke som digital søknad`() = databaseTest {
        val søknadId = UUID.randomUUID()

        testTransaction {
            søknadStore.savePapir(
                PapirSøknadData(
                    fnrBruker = lagFødselsnummer(),
                    soknadId = søknadId,
                    status = Status.ENDELIG_JOURNALFØRT,
                    journalpostid = 2040,
                    navnBruker = "Fornavn Etternavn",
                ),
            )
            val søknad = søknadStore.hentSoknad(søknadId)
            assertEquals(false, søknad?.er_digital)
        }
    }

    @Test
    fun `Kroppsmål og rullestolinfo blir hentet ut`() = databaseTest {
        val søknadId = UUID.randomUUID()

        testTransaction {
            søknadStore.save(mockSøknadMedRullestol(søknadId))
            val søknad = søknadStore.hentSoknad(søknadId)

            assertNotNull(søknad?.søknadsdata?.bruker?.kroppsmaal)
            assertEquals(176, søknad?.søknadsdata?.bruker?.kroppsmaal?.hoyde)
            assertEquals(99, søknad?.søknadsdata?.bruker?.kroppsmaal?.kroppsvekt)
            assertEquals(23, søknad?.søknadsdata?.bruker?.kroppsmaal?.legglengde)
            assertEquals(56, søknad?.søknadsdata?.bruker?.kroppsmaal?.laarlengde)
            assertEquals(23, søknad?.søknadsdata?.bruker?.kroppsmaal?.setebredde)

            assertNotNull(søknad?.søknadsdata?.hjelpemidler?.first()?.rullestolInfo)
            assertEquals(
                SitteputeValg.TrengerSittepute,
                søknad?.søknadsdata?.hjelpemidler?.first()?.rullestolInfo?.sitteputeValg,
            )
            assertEquals(true, søknad?.søknadsdata?.hjelpemidler?.first()?.rullestolInfo?.skalBrukesIBil)
        }
    }
}
