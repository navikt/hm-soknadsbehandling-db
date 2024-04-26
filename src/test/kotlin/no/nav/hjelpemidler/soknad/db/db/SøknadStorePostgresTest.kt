package no.nav.hjelpemidler.soknad.db.db

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.hjelpemidler.soknad.db.domain.BruksarenaBruker
import no.nav.hjelpemidler.soknad.db.domain.Funksjonsnedsettelse
import no.nav.hjelpemidler.soknad.db.domain.LeveringTilleggsinfo
import no.nav.hjelpemidler.soknad.db.domain.PapirSøknadData
import no.nav.hjelpemidler.soknad.db.domain.SitteputeValg
import no.nav.hjelpemidler.soknad.db.domain.SoknadData
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.mockSøknad
import no.nav.hjelpemidler.soknad.db.mockSøknadMedRullestol
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class SøknadStorePostgresTest {

    @Test
    fun `Hent lagret soknad`() {
        val soknadsId = UUID.randomUUID()
        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    mockSøknad(soknadsId),
                )
                val hentSoknad = this.hentSoknad(soknadsId)
                assertEquals("15084300133", hentSoknad?.søknadsdata?.bruker?.fnummer)
                assertEquals("fornavn", hentSoknad?.søknadsdata?.bruker?.fornavn)
                assertEquals("etternavn", hentSoknad?.søknadsdata?.bruker?.etternavn)
                assertEquals("12345678", hentSoknad?.søknadsdata?.bruker?.telefonNummer)
                assertNull(hentSoknad?.søknadsdata?.bruker?.adresse)
                assertNull(hentSoknad?.søknadsdata?.bruker?.postnummer)
                assertEquals("Stedet", hentSoknad?.søknadsdata?.bruker?.poststed)
                assertEquals("formidlerFornavn formidlerEtternavn", hentSoknad?.søknadsdata?.formidler?.navn)
                assertEquals("arbeidssted", hentSoknad?.søknadsdata?.formidler?.arbeidssted)
                assertEquals("stilling", hentSoknad?.søknadsdata?.formidler?.stilling)
                assertEquals("postadresse arbeidssted 1234 poststed", hentSoknad?.søknadsdata?.formidler?.adresse)
                assertEquals("12345678", hentSoknad?.søknadsdata?.formidler?.telefon)
                assertEquals("treffedager", hentSoknad?.søknadsdata?.formidler?.treffesEnklest)
                assertEquals("epost@adad.com", hentSoknad?.søknadsdata?.formidler?.epost)
                assertNull(hentSoknad?.søknadsdata?.oppfolgingsansvarlig)
                assertEquals("Hjemme", hentSoknad?.søknadsdata?.bruker?.boform)
                assertEquals(BruksarenaBruker.DAGLIGLIVET, hentSoknad?.søknadsdata?.bruker?.bruksarena)
                assertEquals(
                    listOf(Funksjonsnedsettelse.BEVEGELSE, Funksjonsnedsettelse.HØRSEL),
                    hentSoknad?.søknadsdata?.bruker?.funksjonsnedsettelser,
                )

                assertEquals(2, hentSoknad?.søknadsdata?.hjelpemiddelTotalAntall)
                assertEquals(1, hentSoknad?.søknadsdata?.hjelpemidler?.size)
                assertEquals(1, hentSoknad?.søknadsdata?.hjelpemidler?.first()?.antall)
                assertEquals("Hjelpemiddelnavn", hentSoknad?.søknadsdata?.hjelpemidler?.first()?.navn)
                assertEquals("beskrivelse", hentSoknad?.søknadsdata?.hjelpemidler?.first()?.beskrivelse)
                assertEquals("Arbeidsstoler", hentSoknad?.søknadsdata?.hjelpemidler?.first()?.hjelpemiddelkategori)
                assertEquals("123456", hentSoknad?.søknadsdata?.hjelpemidler?.first()?.hmsNr)
                assertEquals("Tilleggsinformasjon", hentSoknad?.søknadsdata?.hjelpemidler?.first()?.tilleggsinformasjon)
                assertEquals("1", hentSoknad?.søknadsdata?.hjelpemidler?.first()?.rangering)
                assertEquals(true, hentSoknad?.søknadsdata?.hjelpemidler?.first()?.utlevertFraHjelpemiddelsentralen)
                assertEquals(1, hentSoknad?.søknadsdata?.hjelpemidler?.first()?.vilkarliste?.size)
                assertEquals(
                    "Vilkår 1",
                    hentSoknad?.søknadsdata?.hjelpemidler?.first()?.vilkarliste?.first()?.vilkaarTekst,
                )
                assertEquals(
                    "Tilleggsinfo",
                    hentSoknad?.søknadsdata?.hjelpemidler?.first()?.vilkarliste?.first()?.tilleggsinfo,
                )
                assertEquals(1, hentSoknad?.søknadsdata?.hjelpemidler?.first()?.tilbehorListe?.size)
                assertEquals("654321", hentSoknad?.søknadsdata?.hjelpemidler?.first()?.tilbehorListe?.first()?.hmsnr)
                assertEquals("Tilbehør 1", hentSoknad?.søknadsdata?.hjelpemidler?.first()?.tilbehorListe?.first()?.navn)
                assertEquals(1, hentSoknad?.søknadsdata?.hjelpemidler?.first()?.tilbehorListe?.first()?.antall)
                assertEquals("begrunnelse", hentSoknad?.søknadsdata?.hjelpemidler?.first()?.begrunnelse)
                assertEquals(true, hentSoknad?.søknadsdata?.hjelpemidler?.first()?.kanIkkeTilsvarande)

                assertNull(hentSoknad?.søknadsdata?.levering?.adresse)

                assertEquals(true, hentSoknad?.er_digital)
            }
        }
    }

    @Test
    fun `Hent lagret soknad 2`() {
        val soknadsId = UUID.randomUUID()
        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    SoknadData(
                        "15084300133",
                        "fornavn etternavn",
                        "id2",
                        soknadsId,
                        ObjectMapper().readTree(
                            """ {
                          "fnrBruker": "15084300133",
                          "soknadId": "62f68547-11ae-418c-8ab7-4d2af985bcd9",
                          "datoOpprettet": "2021-02-23T09:46:45.146+00:00",
                          "soknad": {
                            "id": "e8dac11d-fa66-4561-89d7-88a62ab31c2b",
                            "date": "2021-02-16",
                            "bruker": {
                              "kilde": "PDL",
                              "adresse": "Trandemveien 29",
                              "fnummer": "15084300133",
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
                                "utlevertType":"Overført",
                                "overførtFraBruker":"1234"
                              },
                              "tilleggsinfo": ["UTLEVERING_KALENDERAPP"]
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
                        } """,
                        ),
                        status = Status.VENTER_GODKJENNING,
                        kommunenavn = null,
                        er_digital = true,
                        soknadGjelder = null,
                    ),
                )
                val hentSoknad = this.hentSoknad(soknadsId)
                assertEquals("15084300133", hentSoknad!!.søknadsdata!!.bruker.fnummer)
                assertEquals(true, hentSoknad.er_digital)
                assertEquals(LeveringTilleggsinfo.UTLEVERING_KALENDERAPP, hentSoknad.søknadsdata!!.levering.tilleggsinfo.first())
            }
        }
    }

    @Test
    fun `Store soknad`() {
        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    SoknadData(
                        "id",
                        "fornavn etternavn",
                        "id2",
                        UUID.randomUUID(),
                        ObjectMapper().readTree(""" {"key": "value"} """),
                        status = Status.VENTER_GODKJENNING,
                        kommunenavn = null,
                        er_digital = true,
                        soknadGjelder = null,
                    ),
                ).also {
                    it shouldBe 1
                }
            }
        }
    }

    @Test
    fun `Fullmakt for søknad innsendt av formidler`() {
        val soknadId = UUID.randomUUID()

        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    mockSøknad(soknadId, Status.GODKJENT_MED_FULLMAKT),
                ).also {
                    it shouldBe 1
                }

                this.oppdaterStatus(soknadId, Status.ENDELIG_JOURNALFØRT).also {
                    it shouldBe 1
                }

                val soknader = this.hentSoknaderForBruker("15084300133")
                assertEquals(1, soknader.size)
                assertTrue { soknader[0].fullmakt }
            }
        }
    }

    @Test
    fun `Ikke fullmakt for søknad med brukers godkjenning`() {
        val soknadId = UUID.randomUUID()

        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    mockSøknad(soknadId, Status.VENTER_GODKJENNING),
                ).also {
                    it shouldBe 1
                }

                this.oppdaterStatus(soknadId, Status.GODKJENT).also {
                    it shouldBe 1
                }

                this.oppdaterStatus(soknadId, Status.ENDELIG_JOURNALFØRT).also {
                    it shouldBe 1
                }

                val soknader = this.hentSoknaderForBruker("15084300133")
                assertEquals(1, soknader.size)
                assertFalse { soknader[0].fullmakt }
            }
        }
    }

    @Test
    fun `Søknad is utgått`() {
        val id = UUID.randomUUID()

        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    SoknadData(
                        "id",
                        "fornavn etternavn",
                        "id2",
                        id,
                        ObjectMapper().readTree(""" {"key": "value"} """),
                        status = Status.VENTER_GODKJENNING,
                        kommunenavn = null,
                        er_digital = true,
                        soknadGjelder = null,
                    ),
                ).also {
                    it shouldBe 1
                }
            }
            DataSource.instance.apply {
                sessionOf(this).run(queryOf("UPDATE V1_SOKNAD SET CREATED = (now() - interval '2 week') WHERE SOKNADS_ID = '$id' ").asExecute)
            }

            SøknadStorePostgres(DataSource.instance).apply {
                this.hentSoknaderTilGodkjenningEldreEnn(14).also {
                    it.size shouldBe 1
                }
            }

            DataSource.instance.apply {
                sessionOf(this).run(queryOf("UPDATE V1_SOKNAD SET CREATED = (now() - interval '13 day') WHERE SOKNADS_ID = '$id' ").asExecute)
            }

            SøknadStorePostgres(DataSource.instance).apply {
                this.hentSoknaderTilGodkjenningEldreEnn(14).also {
                    it.size shouldBe 0
                }
            }
        }
    }

    @Test
    fun `hent godkjente soknader uten oppgave`() {
        withMigratedDb {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()

            SøknadStorePostgres(DataSource.instance).apply {
                this.save(mockSøknad(id1, Status.GODKJENT))
                this.save(mockSøknad(id2, Status.GODKJENT_MED_FULLMAKT))

                val session = sessionOf(DataSource.instance)
                session.run(queryOf("UPDATE V1_SOKNAD SET CREATED = (now() - interval '2 day') WHERE SOKNADS_ID = '$id1' ").asExecute)
                this.hentGodkjenteBehovsmeldingerUtenOppgaveEldreEnn(2)
                    .also { it.size shouldBe 1 }

                session.run(queryOf("UPDATE V1_SOKNAD SET CREATED = (now() - interval '3 day') WHERE SOKNADS_ID = '$id2' ").asExecute)
                this.hentGodkjenteBehovsmeldingerUtenOppgaveEldreEnn(2)
                    .also { it.size shouldBe 2 }

                session.run(queryOf("UPDATE V1_SOKNAD SET CREATED = (now() - interval '1 day') WHERE SOKNADS_ID = '$id1' ").asExecute)
                this.hentGodkjenteBehovsmeldingerUtenOppgaveEldreEnn(2)
                    .also { it.size shouldBe 1 }

                session.run(queryOf("UPDATE V1_SOKNAD SET oppgaveid = '12345' WHERE SOKNADS_ID = '$id2' ").asExecute)
                this.hentGodkjenteBehovsmeldingerUtenOppgaveEldreEnn(2)
                    .also { it.size shouldBe 0 }
            }
        }
    }

    @Test
    fun `Oppdater oppgaveId og ikke overskriv eksisterende oppgaveId`() {
        val id = UUID.randomUUID()
        val oppgaveId = "57983"
        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    SoknadData(
                        "id",
                        "fornavn etternavn",
                        "id2",
                        id,
                        ObjectMapper().readTree(""" {"key": "value"} """),
                        status = Status.GODKJENT_MED_FULLMAKT,
                        kommunenavn = null,
                        er_digital = true,
                        soknadGjelder = null,
                    ),
                )
            }

            SøknadStorePostgres(DataSource.instance).apply {
                this.oppdaterOppgaveId(id, oppgaveId).also {
                    it shouldBe 1
                }
            }

            SøknadStorePostgres(DataSource.instance).apply {
                this.oppdaterOppgaveId(id, oppgaveId).also {
                    it shouldBe 0
                }
            }
        }
    }

    @Test
    fun `Papirsøknad lagres i databasen`() {
        val id = UUID.randomUUID()
        SøknadStorePostgres(DataSource.instance).apply {
            this.savePapir(
                PapirSøknadData(
                    "12345678910",
                    id,
                    Status.ENDELIG_JOURNALFØRT,
                    1234567,
                    "Person",
                ),
            ).also { it.shouldBe(1) }
        }
    }

    @Test
    fun `Papirsøknad lagres ikke som digital søknad`() {
        val id = UUID.randomUUID()
        SøknadStorePostgres(DataSource.instance).apply {
            this.savePapir(
                PapirSøknadData(
                    "12345678910",
                    id,
                    Status.ENDELIG_JOURNALFØRT,
                    7654321,
                    "Person",
                ),
            )
            val soknad = this.hentSoknad(id)
            assertEquals(false, soknad?.er_digital)
        }
    }

    @Test
    fun `Kroppsmål og rullestolinfo blir hentet ut`() {
        val id = UUID.randomUUID()
        SøknadStorePostgres(DataSource.instance).apply {
            this.save(
                mockSøknadMedRullestol(id),
            )
            val soknad = this.hentSoknad(id)

            assertNotNull(soknad?.søknadsdata?.bruker?.kroppsmaal)
            assertEquals(176, soknad?.søknadsdata?.bruker?.kroppsmaal?.hoyde)
            assertEquals(99, soknad?.søknadsdata?.bruker?.kroppsmaal?.kroppsvekt)
            assertEquals(23, soknad?.søknadsdata?.bruker?.kroppsmaal?.legglengde)
            assertEquals(56, soknad?.søknadsdata?.bruker?.kroppsmaal?.laarlengde)
            assertEquals(23, soknad?.søknadsdata?.bruker?.kroppsmaal?.setebredde)

            assertNotNull(soknad?.søknadsdata?.hjelpemidler?.first()?.rullestolInfo)
            assertEquals(
                SitteputeValg.TrengerSittepute,
                soknad?.søknadsdata?.hjelpemidler?.first()?.rullestolInfo?.sitteputeValg,
            )
            assertEquals(true, soknad?.søknadsdata?.hjelpemidler?.first()?.rullestolInfo?.skalBrukesIBil)
        }
    }
}
