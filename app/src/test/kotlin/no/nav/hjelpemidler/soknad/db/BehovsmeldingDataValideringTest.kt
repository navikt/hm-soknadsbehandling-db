package no.nav.hjelpemidler.soknad.db

import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.string.shouldNotContain
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.serialization.jackson.jsonToTree
import no.nav.hjelpemidler.soknad.db.domain.kommuneapi.v2.Innsenderbehovsmelding
import no.nav.hjelpemidler.soknad.db.domain.lagFødselsnummer
import org.junit.jupiter.api.Test

class BehovsmeldingDataValideringTest {
    @Test
    fun `Validering av en normal søknad i dagens format`() {
        val fnr = lagFødselsnummer()
        lagFødselsnummer()
        val node = jsonToTree(
            """
                {"bruker":{"fnr":"$fnr","navn":{"fornavn":"Sedat","mellomnavn":null,"etternavn":"Kronjuvel"},"signaturtype":"BRUKER_BEKREFTER","telefon":"15084300","veiadresse":{"adresse":"Flataberget 19","postnummer":"4230","poststed":"Sand"},"kommunenummer":"1134","brukernummer":null,"kilde":"PDL","legacyopplysninger":[{"ledetekst":{"nb":"Boform","nn":"Buform"},"innhold":{"nb":"Hjemme","nn":"Heime"}},{"ledetekst":{"nb":"Bruksarena","nn":"Bruksarena"},"innhold":{"nb":"Dagliglivet","nn":"Dagleglivet"}}]},"brukersituasjon":{"vilkår":[{"vilkårtype":"NEDSATT_FUNKSJON","tekst":{"nb":"Sedat Kronjuvel har vesentlig og varig nedsatt funksjonsevne som følge av sykdom, skade eller lyte. Med varig menes 2 år eller livet ut.","nn":"Sedat Kronjuvel har vesentleg og varig nedsett funksjonsevne som følgje av sjukdom, skade eller lyte. Med varig siktar ein til 2 år eller livet ut."}},{"vilkårtype":"STØRRE_BEHOV","tekst":{"nb":"Hjelpemiddelet(ene) er nødvendig for å avhjelpe praktiske problemer i dagliglivet eller bli pleid i hjemmet. Brukers behov kan ikke løses med enklere og rimeligere hjelpemidler eller ved andre tiltak som ikke dekkes av Nav.","nn":"Hjelpemiddelet(a) er naudsynt for å avhjelpa praktiske problem i dagleglivet eller bli pleidd i heimen. Brukars behov kan ikkje løysast med enklare og rimelegare hjelpemiddel eller ved andre tiltak som ikkje blir dekt av Nav."}},{"vilkårtype":"PRAKTISKE_PROBLEM","tekst":{"nb":"Hjelpemiddelet(ene) er egnet til å avhjelpe funksjonsnedsettelsen og Sedat Kronjuvel vil være i stand til å bruke det.","nn":"Hjelpemiddelet(a) er eigna til å avhjelpa funksjonsnedsetjinga og Sedat Kronjuvel vil vera i stand til å bruka det."}}],"funksjonsnedsettelser":["KOGNISJON"],"funksjonsbeskrivelse":null},"hjelpemidler":{"hjelpemidler":[{"hjelpemiddelId":"2509931673972771498","antall":1,"produkt":{"hmsArtNr":"250993","artikkelnavn":"Real 6100 Plus høy","iso8":"12230603","iso8Tittel":"Elektriske rullestoler motorisert styring innebruk","delkontrakttittel":"Post 24: Elektrisk rullestol med motorisert styring for innendørs bruk - voksne","sortimentkategori":"Elektriske rullestoler","delkontraktId":"902","rangering":1},"tilbehør":[],"bytter":[],"bruksarenaer":[],"utlevertinfo":{"alleredeUtlevertFraHjelpemiddelsentralen":false,"utleverttype":null,"overførtFraBruker":null,"annenKommentar":null},"opplysninger":[{"ledetekst":{"nb":"Krav om kurs","nn":"Krav om kurs"},"innhold":[{"fritekst":null,"forhåndsdefinertTekst":{"nb":"Det er dokumentert at innsender har fullført og bestått både del 1 (teoretisk) og del 2 (praktisk) av godkjenningskurs elektrisk rullestol.","nn":"Det er dokumentert at innsendar har fullført og bestått både del 1 (teoretisk) og del 2 (praktisk) av godkjenningskurs elektrisk rullestol."},"begrepsforklaring":null}]},{"ledetekst":{"nb":"Betjene styring","nn":"Betene styring"},"innhold":[{"fritekst":null,"forhåndsdefinertTekst":{"nb":"Brukeren er vurdert til å kunne betjene elektrisk rullestol med motorisert styring","nn":"Brukaren er vurdert til å kunne betene elektrisk rullestol med motorisert styring"},"begrepsforklaring":null}]},{"ledetekst":{"nb":"Gasshendel","nn":"Gasshendel"},"innhold":[{"fritekst":null,"forhåndsdefinertTekst":{"nb":"Skal plasseres på høyre side","nn":"Skal plasserast på høgre side"},"begrepsforklaring":null}]},{"ledetekst":{"nb":"Kroppsmål","nn":"Kroppsmål"},"innhold":[{"fritekst":null,"forhåndsdefinertTekst":{"nb":"Setebredde: 40 cm, lårlengde: 40 cm, legglengde: 40 cm, høyde: 180 cm, kroppsvekt: 80 kg.","nn":"Setebredde: 40 cm, lårlengde: 40 cm, legglengde: 40 cm, høgde: 180 cm, kroppsvekt: 80 kg."},"begrepsforklaring":null}]}],"varsler":[],"saksbehandlingvarsel":[]}],"tilbehør":[],"totaltAntall":1},"levering":{"hjelpemiddelformidler":{"navn":{"fornavn":"Sedat","mellomnavn":null,"etternavn":"Kronjuvel"},"arbeidssted":"Nav Oslo","stilling":"Fysioterapeut","telefon":"12345678","adresse":{"adresse":"Oslo Kommune","postnummer":"0484","poststed":"OSLO"},"epost":"urokkelig@mail.no","treffesEnklest":"Mandag og tirsdag","kommunenavn":null},"oppfølgingsansvarlig":"HJELPEMIDDELFORMIDLER","annenOppfølgingsansvarlig":null,"utleveringsmåte":"FOLKEREGISTRERT_ADRESSE","annenUtleveringsadresse":null,"utleveringKontaktperson":"HJELPEMIDDELBRUKER","annenKontaktperson":null,"utleveringMerknad":"","hast":null,"automatiskUtledetTilleggsinfo":[]},"innsender":{"rolle":"FORMIDLER","erKommunaltAnsatt":null,"kurs":[{"id":1,"title":"El-rullestol","kilde":"kursliste_import"},{"id":2,"title":"Personløftere og seil","kilde":"kunnskapsbanken"},{"id":3,"title":"Elektrisk seng","kilde":"kunnskapsbanken"},{"id":4,"title":"Bestilling","kilde":"kunnskapsbanken"}],"sjekketUtlånsoversiktForKategorier":[]},"metadata":{"bestillingsordningsjekk":{"kanVæreBestilling":false,"kriterier":{"alleHovedProdukterPåBestillingsOrdning":false,"alleTilbehørPåBestillingsOrdning":true,"brukerHarHjelpemidlerFraFør":false,"brukerHarInfotrygdVedtakFraFør":false,"brukerHarHotsakVedtakFraFør":true,"leveringTilFolkeregistrertAdresse":true,"brukersAdresseErSatt":true,"brukerBorIkkeIUtlandet":true,"brukerErIkkeSkjermetPerson":true,"inneholderIkkeFritekst":true,"kildeErPdl":false,"harIkkeForMangeOrdrelinjer":false,"ingenProdukterErAlleredeUtlevert":true,"brukerErTilknyttetBydelIOslo":null,"harIngenBytter":false,"brukerHarAdresseIOeBS":false},"metaInfo":{"hovedProdukter":["250993"],"hovedProdukterIkkePåBestillingsordning":["250993"],"tilbehør":[],"tilbehørIkkePåBestillingsordning":[]},"version":"4d07c217b6647d17f3c3bc39be710a2eaa80d06f"}},"id":"c5e04c1a-d4a4-4626-a046-61a5b894ca5e","type":"SØKNAD","innsendingsdato":"2023-01-17","innsendingstidspunkt":null,"skjemaversjon":2,"hjmBrukersFnr":"07073810927","prioritet":"NORMAL"}
            """.trimIndent(),
        )

        val data = Innsenderbehovsmelding.fraJsonNode(node)
        val filtrert = data.filtrerForKommuneApiet()
        val filtrertRaw = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(filtrert)

        withClue("Forventet at bestillingsordningssjekk ble nullet ut før det sendes til kommunen") {
            filtrert.metadata?.bestillingsordningsjekk.shouldBeNull()
        }

        withClue("Forventet at søknad.innsender ble nullet ut før det sendes til kommunen") {
            filtrert.innsender.shouldBeNull()
        }

        filtrertRaw shouldNotContain "bestillingsordningsjekk"
    }
}
