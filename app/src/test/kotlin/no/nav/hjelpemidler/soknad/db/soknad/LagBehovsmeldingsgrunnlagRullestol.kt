package no.nav.hjelpemidler.soknad.db.soknad

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.Behovsmeldingsgrunnlag
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import no.nav.hjelpemidler.soknad.db.test.readMap
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun mockSøknadMedRullestol(
    id: UUID,
    status: BehovsmeldingStatus = BehovsmeldingStatus.VENTER_GODKJENNING,
    fnrBruker: String = lagFødselsnummer(),
): Behovsmeldingsgrunnlag.Digital = Behovsmeldingsgrunnlag.Digital(
    søknadId = id,
    status = status,
    fnrBruker = fnrBruker,
    navnBruker = "Fornavn Etternavn",
    fnrInnsender = lagFødselsnummer(),
    behovsmelding = readMap(
        """
            {
              "soknad": {
                "timestamp": "${Instant.now()}",
                "date": "${LocalDate.now()}",
                "id": "$id",
                "bruker": {
                  "fnummer": "$fnrBruker",
                  "fornavn": "Fornavn",
                  "etternavn": "Etternavn",
                  "telefonNummer": "12345678",
                  "signatur": "FULLMAKT",
                  "alder": 77,
                  "kroppsmaal": {
                    "setebredde": 23,
                    "laarlengde": 56,
                    "legglengde": 23,
                    "hoyde": 176,
                    "kroppsvekt": 99
                  },
                  "adresse": "Trandemveien 29",
                  "postnummer": "4235",
                  "poststed": "Hebnes",
                  "kilde": "PDL"
                },
                "brukersituasjon": {
                  "nedsattFunksjon": true,
                  "nedsattFunksjonTypes": {
                    "bevegelse": true,
                    "kognisjon": false,
                    "horsel": false
                  },
                  "storreBehov": true,
                  "praktiskeProblem": true,
                  "bostedRadioButton": "Hjemme",
                  "bruksarenaErDagliglivet": true
                },
                "hjelpemidler": {
                  "hjelpemiddelListe": [
                    {
                      "uniqueKey": "1",
                      "hmsNr": "278331",
                      "beskrivelse": "Cross 6 allround sb35 sd35-50 kort",
                      "navn": "Cross 6 allround sb35 sd35-50 kort",
                      "antall": 5,
                      "utlevertFraHjelpemiddelsentralen": false,
                      "tilleggsinformasjon": "Annen kommentar",
                      "hjelpemiddelkategori": "Manuelle rullestoler",
                      "vilkaroverskrift": "",
                      "produkt": {
                        "stockid": "278331",
                        "artid": "118549",
                        "prodid": "64861",
                        "artno": "1.32401e007",
                        "artname": "Cross 6 allround sb35 sd35-50 kort",
                        "adescshort": "",
                        "prodname": "Cross 6 allround ",
                        "pshortdesc": "Cross 6 er en allround rullestol med sammenleggbar ramme. Finnes i kort og lang modell. Stolen har høyderegulerbare kjørehåndtak og avtagbare benstøtter. Leveres uten sittepute. Setehøyde: 46-51 cm. Maks brukervekt: 135 kg. Totalvekt i str. 43, kort: 18,4 kg. Transportvekt (totalvekt minus drivhjul) i str. 43 kort: 14,4 kg.",
                        "artpostid": "19533",
                        "apostid": "1028",
                        "postrank": "1",
                        "apostnr": "5",
                        "aposttitle": "Post 5: Allround rullestol med sammenleggbar ramme og avtakbare benstøtter ",
                        "newsid": "8617",
                        "isocode": "12220302",
                        "isotitle": "Manuelle rullestoler allround",
                        "kategori": "Manuelle rullestoler",
                        "techdataAsText": "Rammetype kryssramme, Setebredde min 35cm, Setebredde maks 35cm, Setedybde min 35cm, Setedybde maks 50cm, Setehøyde min 46cm, Setehøyde maks 51cm, Rygg vinkelstillbar JA, Rygghøyde min 32cm, Rygghøyde maks 51cm, Tilt NEI, Armlener JA, Armlene hreg JA, Kjørehåndtakregulering JA, Drivhjuldiameter 24\", Svinghjuldiameter 6.5\", Totalvekt 17.88kg, Transportvekt 14.03kg, Brukervekt maks 135kg, Beregnet på barn NEI",
                        "haystack": "278331 cross 6 allround sb35 sd35-50 kort post 5: allround rullestol med sammenleggbar ramme og avtakbare benstøtter  manuelle rullestoler allround rammetype kryssramme, setebredde min 35cm, setebredde maks 35cm, setedybde min 35cm, setedybde maks 50cm, setehøyde min 46cm, setehøyde maks 51cm, rygg vinkelstillbar ja, rygghøyde min 32cm, rygghøyde maks 51cm, tilt nei, armlener ja, armlene hreg ja, kjørehåndtakregulering ja, drivhjuldiameter 24\", svinghjuldiameter 6.5\", totalvekt 17.88kg, transportvekt 14.03kg, brukervekt maks 135kg, beregnet på barn nei",
                        "cleanposttitle": "Allround rullestol med sammenleggbar ramme og avtakbare benstøtter ",
                        "cleanTechdataAsText": " Setebredde min 35cm,  Setebredde maks 35cm,  Setedybde min 35cm,  Setedybde maks 50cm,  Setehøyde min 46cm,  Setehøyde maks 51cm"
                      },
                      "tilbehorListe": [
                        {
                          "hmsnr": "123456",
                          "navn": "Tilbehør",
                          "antall": 1
                        }
                      ],
                      "begrunnelsen": "Må ha dette",
                      "kanIkkeTilsvarande": true,
                      "rullestolInfo": {
                        "skalBrukesIBil": true,
                        "sitteputeValg": "TrengerSittepute"
                      }
                    }
                  ],
                  "hjelpemiddelTotaltAntall": 6
                },
                "levering": {
                  "hmfFornavn": "Urokkelig Vertikal",
                  "hmfEtternavn": "Familie",
                  "hmfArbeidssted": "",
                  "hmfStilling": "",
                  "hmfPostadresse": "",
                  "hmfPostnr": "1234",
                  "hmfPoststed": "dwa",
                  "hmfTelefon": "11223344",
                  "hmfTreffesEnklest": "",
                  "hmfEpost": "dwa@dwa",
                  "opfRadioButton": "Hjelpemiddelformidler",
                  "opfFornavn": "",
                  "opfEtternavn": "",
                  "opfTelefon": "",
                  "opfArbeidssted": "",
                  "opfStilling": "",
                  "opfAnsvarFor": "",
                  "utleveringsmaateRadioButton": "FolkeregistrertAdresse",
                  "utleveringPostadresse": "",
                  "utleveringPostnr": "",
                  "utleveringPoststed": "",
                  "utleveringskontaktpersonRadioButton": "Hjelpemiddelbruker",
                  "utleveringFornavn": "",
                  "utleveringEtternavn": "",
                  "utleveringTelefon": "",
                  "merknadTilUtlevering": ""
                }
              }
            }
        """.trimIndent(),
    ),
    behovsmeldingGjelder = null,
)
