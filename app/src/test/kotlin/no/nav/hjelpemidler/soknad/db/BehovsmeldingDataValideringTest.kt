package no.nav.hjelpemidler.soknad.db

import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.string.shouldNotContain
import no.nav.hjelpemidler.soknad.db.domain.kommuneapi.v1.Behovsmelding
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import no.nav.hjelpemidler.soknad.db.soknad.lagSøknadId
import no.nav.hjelpemidler.soknad.db.test.readTree
import org.junit.jupiter.api.Test

class BehovsmeldingDataValideringTest {
    @Test
    fun `Validering av en normal søknad i dagens format`() {
        val søknadId = lagSøknadId()
        val node = readTree(
            """
                {
                  "id": "$søknadId",
                  "soknad": {
                    "id": "$søknadId",
                    "date": "2023-01-17",
                    "bruker": {
                      "kilde": "PDL",
                      "adresse": "Flataberget 19",
                      "fnummer": "${lagFødselsnummer()}",
                      "fornavn": "Sedat",
                      "poststed": "Sand",
                      "signatur": "BRUKER_BEKREFTER",
                      "etternavn": "Kronjuvel",
                      "kroppsmaal": {
                        "hoyde": 180,
                        "kroppsvekt": 80,
                        "laarlengde": 40,
                        "legglengde": 40,
                        "setebredde": 40
                      },
                      "postnummer": "4230",
                      "kommunenummer": "1134",
                      "telefonNummer": "15084300",
                      "erInformertOmRettigheter": false
                    },
                    "levering": {
                      "hmfEpost": "urokkelig@mail.no",
                      "hmfPostnr": "0484",
                      "hmfFornavn": "Sedat",
                      "hmfTelefon": "12345678",
                      "opfFornavn": "",
                      "opfTelefon": "",
                      "hmfPoststed": "OSLO",
                      "hmfStilling": "Fysioterapeut",
                      "opfStilling": "",
                      "hmfEtternavn": "Kronjuvel",
                      "opfAnsvarFor": "",
                      "opfEtternavn": "",
                      "hmfArbeidssted": "NAV Oslo",
                      "hmfPostadresse": "Oslo Kommune",
                      "opfArbeidssted": "",
                      "opfRadioButton": "Hjelpemiddelformidler",
                      "utleveringPostnr": "",
                      "hmfTreffesEnklest": "Mandag og tirsdag",
                      "utleveringFornavn": "",
                      "utleveringTelefon": "",
                      "utleveringPoststed": "",
                      "utleveringEtternavn": "",
                      "merknadTilUtlevering": "",
                      "utleveringPostadresse": "",
                      "utleveringsmaateRadioButton": "FolkeregistrertAdresse",
                      "utleveringskontaktpersonRadioButton": "Hjelpemiddelbruker"
                    },
                    "innsender": {
                      "somRolle": "FORMIDLER",
                      "organisasjoner": [
                        {
                          "navn": "STORÅS OG HESSENG",
                          "orgnr": "910753282",
                          "orgform": "AS"
                        }
                      ],
                      "godkjenningskurs": [
                        {
                          "id": 1,
                          "kilde": "kursliste_import",
                          "title": "El-rullestol"
                        },
                        {
                          "id": 2,
                          "kilde": "kunnskapsbanken",
                          "title": "Personløftere og seil"
                        },
                        {
                          "id": 3,
                          "kilde": "kunnskapsbanken",
                          "title": "Elektrisk seng"
                        },
                        {
                          "id": 4,
                          "kilde": "kunnskapsbanken",
                          "title": "Bestilling"
                        }
                      ]
                    },
                    "hjelpemidler": {
                      "hjelpemiddelListe": [
                        {
                          "navn": "Real 6100 Plus høy",
                          "hmsNr": "250993",
                          "antall": 1,
                          "produkt": {
                            "artid": "113619",
                            "newsid": "7449",
                            "prodid": "61718",
                            "apostid": "902",
                            "apostnr": "24.0",
                            "artname": "Real 6100 Plus høy",
                            "isocode": "12230603",
                            "stockid": "250993",
                            "isotitle": "Elektriske rullestoler motorisert styring innebruk",
                            "kategori": "Elektriske rullestoler",
                            "postrank": "1",
                            "prodname": "Real 6100 Plus høy/lav",
                            "techdata": [],
                            "aposttitle": "Post 24: Elektrisk rullestol med motorisert styring for innendørs bruk - voksne",
                            "pshortdesc": "Elektrisk rullestol med senterdrift for innendørs bruk. Finnes i lav og høy versjon. Elektrisk regulering av seteløft og manuell regulering av ryggvinkel. Leveres som standard med Ergomedic Plus sittenhet. Setebredde: 44 cm. Maks brukervekt: 135 kg.",
                            "techdataAsText": "Setebredde min 38cm, Setebredde maks 52cm, Setedybde min 33cm, Setedybde maks 48cm, Setehøyde uten pute min 48cm, Setehøyde uten pute maks 76cm, Rygghøyde min 32cm, Rygghøyde maks 45cm, Totalbredde 58cm, Totallengde 79cm, Seteløft elektrisk, Tilt ingen, Ryggvinkling manuell, Seterotasjon ingen, Ståfunksjon (elektrisk) NEI, Benstøttevinkling manuell, Armlener høyderegulerbare JA, Armlener dybderegulerbare NEI, Armlener oppfellbare JA, Armlener nedfellbare JA, Benstøtte sentermontert JA, Hjuldrift senterdrift, Hastighet maks 4.5km/t, Kjørelengde maks 15km, Motorstyrke 220watt, Hinderhøyde maks 4cm, Helningsgrad maks 3grader, Vekt inkl batteri 83kg, Brukervekt maks 135kg, Beregnet på barn NEI",
                            "paakrevdGodkjenningskurs": {
                              "kursId": 1,
                              "tittel": "Elektrisk rullestol",
                              "isokode": "122306",
                              "formidlersGjennomforing": "GODKJENNINGSKURS_DB"
                            }
                          },
                          "uniqueKey": "2509931673972771498",
                          "beskrivelse": "Real 6100 Plus høy",
                          "begrunnelsen": "",
                          "utlevertInfo": {},
                          "tilbehorListe": [],
                          "vilkaroverskrift": "",
                          "kanIkkeTilsvarande": "false",
                          "tilleggsinformasjon": "",
                          "hjelpemiddelkategori": "Elektriske rullestoler",
                          "elektriskRullestolInfo": {
                            "plasseringAvHendel": "Høyre",
                            "nedsattGangfunksjon": false,
                            "ferdesSikkertITrafikk": false,
                            "kanBetjeneManuellStyring": false,
                            "kanBetjeneMotorisertStyring": true
                          },
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
                        "bevegelse": false,
                        "kognisjon": true
                      },
                      "bruksarenaErDagliglivet": true,
                      "skalIkkeBrukesTilAndreFormaal": false
                    }
                  },
                  "behovsmeldingType": "SØKNAD",
                  "bestillingsordningsjekk": {
                    "version": "4d07c217b6647d17f3c3bc39be710a2eaa80d06f",
                    "metaInfo": {
                      "tilbehør": [],
                      "hovedProdukter": [
                        "250993"
                      ],
                      "tilbehørIkkePåBestillingsordning": [],
                      "hovedProdukterIkkePåBestillingsordning": [
                        "250993"
                      ]
                    },
                    "kriterier": {
                      "brukersAdresseErSatt": true,
                      "brukerBorIkkeIUtlandet": true,
                      "inneholderIkkeFritekst": true,
                      "brukerErIkkeSkjermetPerson": true,
                      "brukerBorIkkePåInstitusjon": true,
                      "brukerTilhørerPilotsentral": false,
                      "brukerHarHjelpemidlerFraFør": false,
                      "brukerHarHotsakVedtakFraFør": true,
                      "brukerHarInfotrygdVedtakFraFør": false,
                      "ingenProdukterErAlleredeUtlevert": true,
                      "leveringTilFolkeregistrertAdresse": true,
                      "alleTilbehørPåBestillingsOrdning": true,
                      "alleHovedProdukterPåBestillingsOrdning": false
                    },
                    "kanVæreBestilling": false
                  }
                }
            """.trimIndent(),
        )

        val data = Behovsmelding.fraJsonNode(node)
        val filtrert = data.filtrerForKommuneApiet()
        val filtrertRaw = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(filtrert)

        withClue("Forventet at bestillingsordningssjekk ble nullet ut før det sendes til kommunen") {
            filtrert.bestillingsordningsjekk.shouldBeNull()
        }

        withClue("Forventet at søknad.innsender ble nullet ut før det sendes til kommunen") {
            filtrert.soknad.innsender.shouldBeNull()
        }

        filtrertRaw shouldNotContain "bestillingsordningsjekk"
        filtrertRaw shouldNotContain "innsender"
    }
}
