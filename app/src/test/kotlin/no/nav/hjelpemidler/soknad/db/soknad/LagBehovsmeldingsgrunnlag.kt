package no.nav.hjelpemidler.soknad.db.soknad

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingId
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.Behovsmeldingsgrunnlag
import no.nav.hjelpemidler.serialization.jackson.jsonToValue
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import java.time.Instant
import java.time.LocalDate

fun lagBehovsmeldingsgrunnlagDigital(
    søknadId: BehovsmeldingId = lagSøknadId(),
    status: BehovsmeldingStatus = BehovsmeldingStatus.VENTER_GODKJENNING,
    fnrBruker: String = lagFødselsnummer(),
    fnrInnsender: String = lagFødselsnummer(),
    behovsmeldingType: BehovsmeldingType = BehovsmeldingType.SØKNAD,
    erKommunaltAnsatt: Boolean = true,
    innsenderArbeidsstedKommunenummer: String = "9999",
    brukersKommunenummer: String = "9999",
    formidlersEpost: String = "formidler@kommune.no",
): Behovsmeldingsgrunnlag.Digital {
    val v1Json = """
        {
          "soknad": {
          }
        }
    """.trimIndent()
    val v2Json = """
        {
          "bruker": {
            "fnr": "$fnrBruker",
            "navn": {
              "fornavn": "Fornavn",
              "mellomnavn": null,
              "etternavn": "Etternavn"
            },
            "signaturtype": "FULLMAKT",
            "telefon": "12345678",
            "veiadresse": {
              "adresse": "adresseveien 2",
              "postnummer": "1234",
              "poststed": "poststed"
            },
            "kommunenummer": "$brukersKommunenummer",
            "brukernummer": null,
            "kilde": null,
            "legacyopplysninger": [
              {
                "ledetekst": { "nb": "Boform", "nn": "Buform" },
                "innhold": { "nb": "Hjemme", "nn": "Heime" }
              },
              {
                "ledetekst": { "nb": "Bruksarena", "nn": "Bruksarena" },
                "innhold": { "nb": "Dagliglivet", "nn": "Dagleglivet" }
              }
            ]
          },
          "brukersituasjon": {
            "vilkår": [],
            "funksjonsnedsettelser": ["BEVEGELSE", "HØRSEL"],
            "funksjonsbeskrivelse": null
          },
          "hjelpemidler": {
            "hjelpemidler": [
              {
                "hjelpemiddelId": "1",
                "antall": 1,
                "produkt": {
                  "hmsArtNr": "123456",
                  "artikkelnavn": "Hjelpemiddelnavn",
                  "iso8": "11111111",
                  "iso8Tittel": "Isotittel",
                  "delkontrakttittel": "Delkontrakt",
                  "sortimentkategori": "Arbeidsstoler",
                  "delkontraktId": null,
                  "rangering": 1
                },
                "tilbehør": [
                  {
                    "hmsArtNr": "654321",
                    "navn": "Tilbehør 1",
                    "antall": 1,
                    "begrunnelse": null,
                    "fritakFraBegrunnelseÅrsak": null,
                    "opplysninger": [],
                    "saksbehandlingvarsel": []
                  }
                ],
                "bytter": [],
                "bruksarenaer": [],
                "utlevertinfo": {
                  "alleredeUtlevertFraHjelpemiddelsentralen": true,
                  "utleverttype": null,
                  "overførtFraBruker": null,
                  "annenKommentar": null
                },
                "opplysninger": [
                  {
                    "ledetekst": { "nb": "Behov", "nn": "Behov" },
                    "innhold": [
                      {
                        "fritekst": "Tilleggsinfo",
                        "forhåndsdefinertTekst": null,
                        "begrepsforklaring": null
                      }
                    ]
                  },
                  {
                    "ledetekst": {
                      "nb": "Kan ikke ha tilsvarende fordi",
                      "nn": "Kan ikkje ha tilsvarande fordi"
                    },
                    "innhold": [
                      {
                        "fritekst": "begrunnelse",
                        "forhåndsdefinertTekst": null,
                        "begrepsforklaring": null
                      }
                    ]
                  },
                  {
                    "ledetekst": { "nb": "Kommentar", "nn": "Kommentar" },
                    "innhold": [
                      {
                        "fritekst": "Tilleggsinformasjon",
                        "forhåndsdefinertTekst": null,
                        "begrepsforklaring": null
                      }
                    ]
                  }
                ],
                "varsler": [],
                "saksbehandlingvarsel": []
              }
            ],
            "tilbehør": [],
            "totaltAntall": 2
          },
          "levering": {
            "hjelpemiddelformidler": {
              "navn": {
                "fornavn": "formidlerFornavn",
                "mellomnavn": null,
                "etternavn": "formidlerEtternavn"
              },
              "arbeidssted": "arbeidssted",
              "stilling": "stilling",
              "telefon": "12345678",
              "adresse": {
                "adresse": "postadresse arbeidssted",
                "postnummer": "9999",
                "poststed": "poststed"
              },
              "epost": "$formidlersEpost",
              "treffesEnklest": "treffesEnklest",
              "kommunenavn": null,
              "kommunenummer": "$innsenderArbeidsstedKommunenummer"
            },
            "oppfølgingsansvarlig": "HJELPEMIDDELFORMIDLER",
            "annenOppfølgingsansvarlig": null,
            "utleveringsmåte": "FOLKEREGISTRERT_ADRESSE",
            "annenUtleveringsadresse": null,
            "utleveringKontaktperson": "HJELPEMIDDELBRUKER",
            "annenKontaktperson": null,
            "utleveringMerknad": "",
            "hast": null,
            "automatiskUtledetTilleggsinfo": []
          },
          "innsender": {
            "rolle": "FORMIDLER",
            "erKommunaltAnsatt": $erKommunaltAnsatt,
            "kurs": [],
            "sjekketUtlånsoversiktForKategorier": []
          },
          "metadata": { "bestillingsordningsjekk": null },
          "id": "$søknadId",
          "type": "$behovsmeldingType",
          "innsendingsdato": "${LocalDate.now()}",
          "innsendingstidspunkt": "${Instant.now()}",
          "skjemaversjon": 2,
          "hjmBrukersFnr": "$fnrBruker",
          "prioritet": "NORMAL"
        }
    """.trimIndent()
    return Behovsmeldingsgrunnlag.Digital(
        søknadId = søknadId,
        status = status,
        fnrBruker = fnrBruker,
        navnBruker = "Fornavn Etternavn",
        fnrInnsender = fnrInnsender,
        behovsmelding = jsonToValue(v1Json),
        behovsmeldingGjelder = "TEST",
        behovsmeldingV2 = jsonToValue(v2Json),
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
