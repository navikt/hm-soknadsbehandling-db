package no.nav.hjelpemidler.soknad.db.soknad

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.Behovsmeldingsgrunnlag
import no.nav.hjelpemidler.serialization.jackson.jsonToValue
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import java.time.LocalDate

fun lagBehovsmeldingsgrunnlagDigital(
    søknadId: BehovsmeldingId = lagSøknadId(),
    status: BehovsmeldingStatus = BehovsmeldingStatus.VENTER_GODKJENNING,
    fnrBruker: String = lagFødselsnummer(),
    fnrInnsender: String = lagFødselsnummer(),
    behovsmeldingType: BehovsmeldingType = BehovsmeldingType.SØKNAD,
): Behovsmeldingsgrunnlag.Digital {
    return Behovsmeldingsgrunnlag.Digital(
        søknadId = søknadId,
        status = status,
        fnrBruker = fnrBruker,
        navnBruker = "Fornavn Etternavn",
        fnrInnsender = fnrInnsender,
        behovsmelding = jsonToValue(
            """
                {
                  "id": "$søknadId",
                  "behovsmeldingType": "$behovsmeldingType",
                  "soknad": {
                    "id": "$søknadId",
                    "date": "${LocalDate.now()}",
                    "bruker": {
                      "fnummer": "$fnrBruker",
                      "fornavn": "Fornavn",
                      "signatur": "FULLMAKT",
                      "etternavn": "Etternavn",
                      "telefonNummer": "12345678",
                      "adresse": "adresseveien 2",
                      "postnummer": "1234",
                      "poststed": "poststed",
                      "kommunenummer": "9999",
                      "kroppsmaal": {}
                    },
                    "brukersituasjon": {
                      "bostedRadioButton": "Hjemme",
                      "bruksarenaErDagliglivet": true,
                      "nedsattFunksjonTypes": {
                        "bevegelse": true,
                        "kognisjon": false,
                        "horsel": true
                      }
                    },
                    "hjelpemidler": {
                      "hjelpemiddelTotaltAntall": 2,
                      "hjelpemiddelListe": [
                        {
                          "uniqueKey": "1",
                          "hmsNr": "123456",
                          "beskrivelse": "Hjelpemiddelnavn",
                          "begrunnelsen": "begrunnelse",
                          "antall": 1,
                          "navn": "Hjelpemiddelnavn",
                          "utlevertFraHjelpemiddelsentralen": true,
                          "tilleggsinformasjon": "Tilleggsinformasjon",
                          "kanIkkeTilsvarande": true,
                          "hjelpemiddelkategori": "Arbeidsstoler",
                          "produkt": {
                            "postrank": "1",
                            "isocode": "11111111",
                            "isotitle": "Isotittel",
                            "aposttitle": "Delkontrakt",
                            "kategori": "Arbeidsstoler"
                          },
                          "vilkarliste": [
                            {
                              "vilkartekst": "Vilkår 1",
                              "tilleggsinfo": "Tilleggsinfo",
                              "checked": true
                            }
                          ],
                          "tilbehorListe": [
                            {
                              "hmsnr": "654321",
                              "navn": "Tilbehør 1",
                              "antall": 1
                            }
                          ]
                        }
                      ]
                    },
                    "levering": {
                      "hmfFornavn": "formidlerFornavn",
                      "hmfEtternavn": "formidlerEtternavn",
                      "hmfArbeidssted": "arbeidssted",
                      "hmfStilling": "stilling",
                      "hmfPostadresse": "postadresse arbeidssted",
                      "hmfPostnr": "9999",
                      "hmfPoststed": "poststed",
                      "hmfTelefon": "12345678",
                      "hmfTreffesEnklest": "treffesEnklest",
                      "hmfEpost": "formidler@kommune.no",
                      "opfRadioButton": "Hjelpemiddelformidler",
                      "utleveringsmaateRadioButton": "FolkeregistrertAdresse",
                      "utleveringskontaktpersonRadioButton": "Hjelpemiddelbruker",
                      "merknadTilUtlevering": ""
                    },
                    "innsender": {
                      "somRolle": "FORMIDLER",
                      "erKommunaltAnsatt": true,
                      "organisasjoner": [
                        {
                          "navn": "STORÅS OG HESSENG",
                          "orgnr": "910753282",
                          "orgform": "AS",
                          "kommunenummer": "9999"
                        }
                      ],
                      "godkjenningskurs": []
                    }
                  }
                }
            """.trimIndent(),
        ),
        behovsmeldingGjelder = "TEST",
    )
}

fun lagBehovsmeldingsgrunnlagPapir(
    søknadId: BehovsmeldingId = lagSøknadId(),
    status: BehovsmeldingStatus = BehovsmeldingStatus.ENDELIG_JOURNALFØRT,
    fnrBruker: String = lagFødselsnummer(),
): Behovsmeldingsgrunnlag.Papir {
    return Behovsmeldingsgrunnlag.Papir(
        søknadId = søknadId,
        status = status,
        fnrBruker = fnrBruker,
        navnBruker = "Fornavn Etternavn",
        journalpostId = (1_000_000..9_999_999).random().toString(),
        sakstilknytning = lagSakstilknytningInfotrygd(fnrBruker),
    )
}
