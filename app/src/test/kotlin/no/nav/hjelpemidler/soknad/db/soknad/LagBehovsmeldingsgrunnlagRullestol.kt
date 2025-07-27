package no.nav.hjelpemidler.soknad.db.soknad

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.Behovsmeldingsgrunnlag
import no.nav.hjelpemidler.serialization.jackson.jsonToValue
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import java.util.UUID

fun mockSøknadMedRullestol(
    id: UUID,
    status: BehovsmeldingStatus = BehovsmeldingStatus.VENTER_GODKJENNING,
    fnrBruker: String = lagFødselsnummer(),
): Behovsmeldingsgrunnlag.Digital {
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
              "adresse": "Trandemveien 29",
              "postnummer": "4235",
              "poststed": "Hebnes"
            },
            "kommunenummer": null,
            "brukernummer": null,
            "kilde": "PDL",
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
            "vilkår": [
              {
                "vilkårtype": "NEDSATT_FUNKSJON",
                "tekst": {
                  "nb": "Fornavn Etternavn har vesentlig og varig nedsatt funksjonsevne som følge av sykdom, skade eller lyte. Med varig menes 2 år eller livet ut.",
                  "nn": "Fornavn Etternavn har vesentleg og varig nedsett funksjonsevne som følgje av sjukdom, skade eller lyte. Med varig siktar ein til 2 år eller livet ut."
                }
              },
              {
                "vilkårtype": "STØRRE_BEHOV",
                "tekst": {
                  "nb": "Hjelpemiddelet(ene) er nødvendig for å avhjelpe praktiske problemer i dagliglivet eller bli pleid i hjemmet. Brukers behov kan ikke løses med enklere og rimeligere hjelpemidler eller ved andre tiltak som ikke dekkes av Nav.",
                  "nn": "Hjelpemiddelet(a) er naudsynt for å avhjelpa praktiske problem i dagleglivet eller bli pleidd i heimen. Brukars behov kan ikkje løysast med enklare og rimelegare hjelpemiddel eller ved andre tiltak som ikkje blir dekt av Nav."
                }
              },
              {
                "vilkårtype": "PRAKTISKE_PROBLEM",
                "tekst": {
                  "nb": "Hjelpemiddelet(ene) er egnet til å avhjelpe funksjonsnedsettelsen og Fornavn Etternavn vil være i stand til å bruke det.",
                  "nn": "Hjelpemiddelet(a) er eigna til å avhjelpa funksjonsnedsetjinga og Fornavn Etternavn vil vera i stand til å bruka det."
                }
              }
            ],
            "funksjonsnedsettelser": ["BEVEGELSE"],
            "funksjonsbeskrivelse": null
          },
          "hjelpemidler": {
            "hjelpemidler": [
              {
                "hjelpemiddelId": "1",
                "antall": 5,
                "produkt": {
                  "hmsArtNr": "278331",
                  "artikkelnavn": "Cross 6 allround sb35 sd35-50 kort",
                  "iso8": "12220302",
                  "iso8Tittel": "Manuelle rullestoler allround",
                  "delkontrakttittel": "Post 5: Allround rullestol med sammenleggbar ramme og avtakbare benstøtter ",
                  "sortimentkategori": "Manuelle rullestoler",
                  "delkontraktId": "1028",
                  "rangering": 1
                },
                "tilbehør": [
                  {
                    "hmsArtNr": "123456",
                    "navn": "Tilbehør",
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
                  "alleredeUtlevertFraHjelpemiddelsentralen": false,
                  "utleverttype": null,
                  "overførtFraBruker": null,
                  "annenKommentar": null
                },
                "opplysninger": [
                  {
                    "ledetekst": {
                      "nb": "Kan ikke ha tilsvarende fordi",
                      "nn": "Kan ikkje ha tilsvarande fordi"
                    },
                    "innhold": [
                      {
                        "fritekst": "Må ha dette",
                        "forhåndsdefinertTekst": null,
                        "begrepsforklaring": null
                      }
                    ]
                  },
                  {
                    "ledetekst": { "nb": "Bil", "nn": "Bil" },
                    "innhold": [
                      {
                        "fritekst": null,
                        "forhåndsdefinertTekst": {
                          "nb": "Rullestolen skal brukes som sete i bil",
                          "nn": "Rullestolen skal brukast som sete i bil"
                        },
                        "begrepsforklaring": null
                      }
                    ]
                  },
                  {
                    "ledetekst": { "nb": "Sittepute", "nn": "Sitjepute" },
                    "innhold": [
                      {
                        "fritekst": null,
                        "forhåndsdefinertTekst": {
                          "nb": "Bruker skal ha sittepute",
                          "nn": "Brukar skal ha sitjepute"
                        },
                        "begrepsforklaring": null
                      }
                    ]
                  },
                  {
                    "ledetekst": { "nb": "Kommentar", "nn": "Kommentar" },
                    "innhold": [
                      {
                        "fritekst": "Annen kommentar",
                        "forhåndsdefinertTekst": null,
                        "begrepsforklaring": null
                      }
                    ]
                  },
                  {
                    "ledetekst": { "nb": "Kroppsmål", "nn": "Kroppsmål" },
                    "innhold": [
                      {
                        "fritekst": null,
                        "forhåndsdefinertTekst": {
                          "nb": "Setebredde: 23 cm, lårlengde: 56 cm, legglengde: 23 cm, høyde: 176 cm, kroppsvekt: 99 kg.",
                          "nn": "Setebredde: 23 cm, lårlengde: 56 cm, legglengde: 23 cm, høgde: 176 cm, kroppsvekt: 99 kg."
                        },
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
            "totaltAntall": 6
          },
          "levering": {
            "hjelpemiddelformidler": {
              "navn": {
                "fornavn": "Urokkelig Vertikal",
                "mellomnavn": null,
                "etternavn": "Familie"
              },
              "arbeidssted": "",
              "stilling": "",
              "telefon": "11223344",
              "adresse": {
                "adresse": "Testveien 1",
                "postnummer": "1234",
                "poststed": "Teststedet"
              },
              "epost": "dwa@dwa",
              "treffesEnklest": "",
              "kommunenavn": null
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
            "erKommunaltAnsatt": null,
            "kurs": [],
            "sjekketUtlånsoversiktForKategorier": []
          },
          "metadata": { "bestillingsordningsjekk": null },
          "id": "$id",
          "type": "SØKNAD",
          "innsendingsdato": "2025-07-27",
          "innsendingstidspunkt": null,
          "skjemaversjon": 2,
          "hjmBrukersFnr": "$fnrBruker",
          "prioritet": "NORMAL"
        }
    """.trimIndent()
    return Behovsmeldingsgrunnlag.Digital(
        søknadId = id,
        status = status,
        fnrBruker = fnrBruker,
        navnBruker = "Fornavn Etternavn",
        fnrInnsender = lagFødselsnummer(),
        behovsmelding = emptyMap(),
        behovsmeldingGjelder = null,
        behovsmeldingV2 = jsonToValue(v2Json),
    )
}
