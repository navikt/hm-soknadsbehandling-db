package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import no.nav.hjelpemidler.soknad.db.JacksonMapper.Companion.objectMapper
import no.nav.hjelpemidler.soknad.db.client.hmdb.hentproduktermedhmsnrs.Produkt
import java.util.Date
import java.util.UUID

class SøknadForBruker private constructor(
    val søknadId: UUID,
    val behovsmeldingType: BehovsmeldingType,
    val journalpostId: String?,
    val datoOpprettet: Date,
    var datoOppdatert: Date,
    val status: Status,
    val fullmakt: Boolean,
    val fnrBruker: String,
    val søknadsdata: Søknadsdata?,
    val er_digital: Boolean,
    val soknadGjelder: String?,
    var ordrelinjer: List<SøknadForBrukerOrdrelinje>,
    var fagsakId: String?,
    var søknadType: String?,
    val valgteÅrsaker: List<String>,
) {
    companion object {
        fun new(
            søknadId: UUID,
            behovsmeldingType: BehovsmeldingType,
            journalpostId: String?,
            datoOpprettet: Date,
            datoOppdatert: Date,
            søknad: JsonNode,
            status: Status,
            fullmakt: Boolean,
            kommunenavn: String?,
            fnrBruker: String,
            er_digital: Boolean,
            soknadGjelder: String?,
            ordrelinjer: List<SøknadForBrukerOrdrelinje>,
            fagsakId: String?,
            søknadType: String?,
            valgteÅrsaker: List<String>,
        ) =
            SøknadForBruker(
                søknadId,
                behovsmeldingType,
                journalpostId,
                datoOpprettet,
                datoOppdatert,
                status,
                fullmakt,
                fnrBruker,
                Søknadsdata(søknad, kommunenavn),
                er_digital,
                soknadGjelder,
                ordrelinjer,
                fagsakId,
                søknadType,
                valgteÅrsaker,
            )

        fun newEmptySøknad(
            søknadId: UUID,
            behovsmeldingType: BehovsmeldingType,
            journalpostId: String?,
            datoOpprettet: Date,
            datoOppdatert: Date,
            status: Status,
            fullmakt: Boolean,
            fnrBruker: String,
            er_digital: Boolean,
            soknadGjelder: String?,
            ordrelinjer: List<SøknadForBrukerOrdrelinje>,
            fagsakId: String?,
            søknadType: String?,
            valgteÅrsaker: List<String>,
        ) =
            SøknadForBruker(
                søknadId,
                behovsmeldingType,
                journalpostId,
                datoOpprettet,
                datoOppdatert,
                status,
                fullmakt,
                fnrBruker,
                null,
                er_digital,
                soknadGjelder,
                ordrelinjer,
                fagsakId,
                søknadType,
                valgteÅrsaker,
            )
    }
}

private val bekreftedeVilkårReader =
    objectMapper.readerFor(object : TypeReference<List<BrukersituasjonVilkår>?>() {})

private fun bruker(søknad: JsonNode): Bruker {
    val brukerNode = søknad["soknad"]["bruker"]
    val brukerSituasjonNode = søknad["soknad"]["brukersituasjon"]
    val storreBehov = brukerSituasjonNode["storreBehov"]?.booleanValue() ?: false
    val praktiskeProblem = brukerSituasjonNode["praktiskeProblem"]?.booleanValue() ?: false
    val nedsattFunksjon = brukerSituasjonNode["nedsattFunksjon"]?.booleanValue() ?: false
    val skalIkkeBrukesTilAndreFormaal = brukerSituasjonNode["skalIkkeBrukesTilAndreFormaal"]?.booleanValue() ?: false
    val bruksarenaErDagliglivet = brukerSituasjonNode["bruksarenaErDagliglivet"]?.booleanValue() ?: false

    val bekreftedeVilkår: List<BrukersituasjonVilkår> =
        brukerSituasjonNode["bekreftedeVilkår"]?.let { bekreftedeVilkårReader.readValue(it) }
            ?: mutableListOf<BrukersituasjonVilkår>().apply {
                // Håndter eldre variant av datamodellen
                if (skalIkkeBrukesTilAndreFormaal) {
                    // Bestilling
                    add(BrukersituasjonVilkår.VESENTLIG_OG_VARIG_NEDSATT_FUNKSJONSEVNE_V1)
                    if (storreBehov) add(BrukersituasjonVilkår.KAN_IKKE_LOESES_MED_ENKLERE_HJELPEMIDLER_V1)
                    if (praktiskeProblem) add(BrukersituasjonVilkår.I_STAND_TIL_AA_BRUKE_HJELEPMIDLENE_V1)
                    if (bruksarenaErDagliglivet) add(BrukersituasjonVilkår.PRAKTISKE_PROBLEMER_I_DAGLIGLIVET_V1)
                } else {
                    // Formidler søknad
                    if (storreBehov) add(BrukersituasjonVilkår.STORRE_BEHOV)
                    if (praktiskeProblem) add(BrukersituasjonVilkår.PRAKTISKE_PROBLEM)
                    if (nedsattFunksjon) add(BrukersituasjonVilkår.NEDSATT_FUNKSJON)
                }
            }

    return Bruker(
        fnummer = brukerNode["fnummer"].textValue(),
        fornavn = brukerNode["fornavn"].textValue(),
        etternavn = brukerNode["etternavn"].textValue(),
        telefonNummer = brukerNode["telefonNummer"].textValue(),
        adresse = brukerNode["adresse"]?.textValue(),
        postnummer = brukerNode["postnummer"]?.textValue(),
        poststed = brukerNode["poststed"]?.textValue(),
        boform = brukerSituasjonNode["bostedRadioButton"].textValue(),
        bruksarena = if (søknad["soknad"]["brukersituasjon"]["bruksarenaErDagliglivet"].booleanValue()) Bruksarena.DAGLIGLIVET else Bruksarena.UKJENT,
        funksjonsnedsettelser = funksjonsnedsettelser(søknad),
        signatur = signaturType(søknad),
        kroppsmaal = kroppsmaal(brukerNode),
        brukernummer = brukerNode["brukernummer"]?.textValue(),
        bekreftedeVilkår = bekreftedeVilkår,
    )
}

fun kroppsmaal(brukerNode: JsonNode): Kroppsmaal? {
    val kroppsmaal = brukerNode["kroppsmaal"] ?: return null

    return Kroppsmaal(
        setebredde = kroppsmaal["setebredde"]?.intValue(),
        laarlengde = kroppsmaal["laarlengde"]?.intValue(),
        legglengde = kroppsmaal["legglengde"]?.intValue(),
        hoyde = kroppsmaal["hoyde"]?.intValue(),
        kroppsvekt = kroppsmaal["kroppsvekt"]?.intValue(),
    )
}

private fun formidler(søknad: JsonNode, kommunenavn: String?): Formidler {
    val leveringNode = søknad["soknad"]["levering"]
    return Formidler(
        navn = "${leveringNode["hmfFornavn"].textValue()} ${leveringNode["hmfEtternavn"].textValue()}",
        arbeidssted = leveringNode["hmfArbeidssted"].textValue(),
        stilling = leveringNode["hmfStilling"].textValue(),
        adresse = "${leveringNode["hmfPostadresse"].textValue()} ${leveringNode["hmfPostnr"].textValue()} ${leveringNode["hmfPoststed"].textValue()}",
        telefon = leveringNode["hmfTelefon"].textValue(),
        treffesEnklest = leveringNode["hmfTreffesEnklest"].textValue(),
        epost = leveringNode["hmfEpost"].textValue(),
        kommunenavn = kommunenavn,
    )
}

private fun oppfolgingsansvarlig(søknad: JsonNode): Oppfolgingsansvarlig? {
    val leveringNode = søknad["soknad"]["levering"]

    if (leveringNode["opfRadioButton"].textValue() == "Hjelpemiddelformidler") {
        return null
    }

    return Oppfolgingsansvarlig(
        navn = "${leveringNode["opfFornavn"].textValue()} ${leveringNode["opfEtternavn"].textValue()}",
        arbeidssted = leveringNode["opfArbeidssted"].textValue(),
        stilling = leveringNode["opfStilling"].textValue(),
        telefon = leveringNode["opfTelefon"].textValue(),
        ansvarFor = leveringNode["opfAnsvarFor"].textValue()
    )
}

private val leveringTilleggsinfoReader =
    objectMapper.readerFor(object : TypeReference<List<LeveringTilleggsinfo>?>() {})

private fun levering(søknad: JsonNode): Levering {
    val leveringNode = søknad["soknad"]["levering"]
    val leveringsMaate = leveringsMaate(søknad)
    return Levering(
        leveringsmaate = leveringsMaate,
        adresse = if (leveringsMaate == Leveringsmaate.ANNEN_ADRESSE) "${leveringNode["utleveringPostadresse"].textValue()} ${leveringNode["utleveringPostnr"].textValue()} ${leveringNode["utleveringPoststed"].textValue()}" else null,
        kontaktPerson = kontaktPerson(søknad),
        merknad = leveringNode["merknadTilUtlevering"]?.textValue(),
        tilleggsinfo = leveringNode["tilleggsinfo"]?.let { leveringTilleggsinfoReader.readValue(it) } ?: emptyList()
    )
}

private fun kontaktPerson(søknad: JsonNode): KontaktPerson {
    val leveringNode = søknad["soknad"]["levering"]
    val kontaktPersonType = kontaktPersonType(søknad)

    return if (kontaktPersonType == KontaktpersonType.ANNEN_KONTAKTPERSON) {
        KontaktPerson(
            navn = "${leveringNode["utleveringFornavn"].textValue()} ${leveringNode["utleveringEtternavn"].textValue()}",
            telefon = leveringNode["utleveringTelefon"].textValue(),
            kontaktpersonType = kontaktPersonType
        )
    } else {
        KontaktPerson(
            kontaktpersonType = kontaktPersonType
        )
    }
}

private fun kontaktPersonType(søknad: JsonNode): KontaktpersonType {
    val leveringNode = søknad["soknad"]["levering"]

    return when (leveringNode["utleveringskontaktpersonRadioButton"]?.textValue()) {
        "Hjelpemiddelbruker" -> KontaktpersonType.HJELPEMIDDELBRUKER
        "Hjelpemiddelformidler" -> KontaktpersonType.HJELPEMIDDELFORMIDLER
        "AnnenKontaktperson" -> KontaktpersonType.ANNEN_KONTAKTPERSON
        else -> KontaktpersonType.INGEN_KONTAKTPERSON
    }
}

private fun signaturType(søknad: JsonNode): SignaturType {
    val brukerNode = søknad["soknad"]["bruker"]

    return when (brukerNode["signatur"].textValue()) {
        "BRUKER_BEKREFTER" -> SignaturType.BRUKER_BEKREFTER
        "FULLMAKT" -> SignaturType.FULLMAKT
        "FRITAK_FRA_FULLMAKT" -> SignaturType.FRITAK_FRA_FULLMAKT
        else -> throw RuntimeException("Ugyldig signaturtype")
    }
}

private fun leveringsMaate(søknad: JsonNode): Leveringsmaate? {
    val leveringNode = søknad["soknad"]["levering"]

    return when (leveringNode["utleveringsmaateRadioButton"]?.textValue()) {
        "AnnenBruksadresse" -> Leveringsmaate.ANNEN_ADRESSE
        "FolkeregistrertAdresse" -> Leveringsmaate.FOLKEREGISTRERT_ADRESSE
        "Hjelpemiddelsentralen" -> Leveringsmaate.HJELPEMIDDELSENTRAL
        "AlleredeUtlevertAvNav" -> Leveringsmaate.ALLEREDE_LEVERT
        null -> null
        else -> throw RuntimeException("Ugyldig leveringsmåte")
    }
}

private fun funksjonsnedsettelser(søknad: JsonNode): List<Funksjonsnedsettelse> {
    val funksjonsnedsettelser = mutableListOf<Funksjonsnedsettelse>()

    val funksjonsnedsettelseNode = søknad["soknad"]["brukersituasjon"]["nedsattFunksjonTypes"]
    if (funksjonsnedsettelseNode["bevegelse"].booleanValue()) funksjonsnedsettelser.add(Funksjonsnedsettelse.BEVEGELSE)
    if (funksjonsnedsettelseNode["kognisjon"].booleanValue()) funksjonsnedsettelser.add(Funksjonsnedsettelse.KOGNISJON)
    if (funksjonsnedsettelseNode["horsel"].booleanValue()) funksjonsnedsettelser.add(Funksjonsnedsettelse.HØRSEL)

    return funksjonsnedsettelser
}

private fun hjelpemidler(søknad: JsonNode): List<Hjelpemiddel> {
    val hjelpemidler = mutableListOf<Hjelpemiddel>()
    søknad["soknad"]["hjelpemidler"]["hjelpemiddelListe"].forEach {
        val hjelpemiddel = Hjelpemiddel(
            antall = it["antall"].intValue(),
            arsakForAntall = arsakForAntall(it),
            arsakForAntallBegrunnelse = it["arsakForAntallBegrunnelse"]?.textValue(),
            beskrivelse = it["beskrivelse"].textValue(),
            hjelpemiddelkategori = it["hjelpemiddelkategori"].textValue(),
            hmsNr = it["hmsNr"].textValue(),
            tilleggsinformasjon = it["tilleggsinformasjon"].textValue(),
            rangering = it["produkt"]["postrank"].textValue(),
            utlevertFraHjelpemiddelsentralen = it["utlevertFraHjelpemiddelsentralen"].booleanValue(),
            vilkarliste = vilkaar(it),
            tilbehorListe = tilbehor(it),
            begrunnelse = it["begrunnelsen"]?.textValue(),
            kanIkkeTilsvarande = it["kanIkkeTilsvarande"].booleanValue(),
            navn = it["navn"]?.textValue(),
            rullestolInfo = rullestolinfo(it),
            elektriskRullestolInfo = elektriskRullestolInfo(it),
            personlofterInfo = personlofterInfo(it),
            utlevertInfo = utlevertInfo(it),
            appInfo = appInfo(it),
        )
        hjelpemidler.add(hjelpemiddel)
    }
    return hjelpemidler
}

private fun arsakForAntall(hjelpemiddel: JsonNode): String? {
    val arsak = hjelpemiddel["arsakForAntall"]?.let {
        when (hjelpemiddel["arsakForAntall"].textValue()) {
            // Returner enums så det blir lettere å legge inn translations
            "Behov i flere etasjer" -> "BEHOV_I_FLERE_ETASJER"
            "Behov i flere rom" -> "BEHOV_I_FLERE_ROM"
            "Behov både innendørs og utendørs" -> "BEHOV_INNENDØRS_OG_UTENDØRS"
            "Behov for pute til flere rullestoler eller sitteenheter" -> "BEHOV_FOR_FLERE_PUTER_FOR_RULLESTOL"
            "Behov for jevnlig vask eller vedlikehold" -> "BEHOV_FOR_JEVNLIG_VASK_ELLER_VEDLIKEHOLD"
            "Bruker har to hjem" -> "BRUKER_HAR_TO_HJEM"
            "Annet behov" -> "ANNET_BEHOV"
            else -> "UKJENT_ÅRSAK"
        }
    }

    return arsak
}

private fun vilkaar(hjelpemiddel: JsonNode): List<HjelpemiddelVilkar> {
    val vilkarListe = mutableListOf<HjelpemiddelVilkar>()
    hjelpemiddel["vilkarliste"]?.forEach {
        vilkarListe.add(
            HjelpemiddelVilkar(
                vilkaarTekst = it["vilkartekst"].textValue(),
                tilleggsInfo = it["tilleggsinfo"]?.textValue()
            )
        )
    }
    return vilkarListe
}

private fun tilbehor(hjelpemiddel: JsonNode): List<Tilbehor> {
    val tilbehorListe = mutableListOf<Tilbehor>()
    hjelpemiddel["tilbehorListe"]?.forEach {
        tilbehorListe.add(
            Tilbehor(
                hmsnr = it["hmsnr"].textValue(),
                navn = it["navn"].textValue(),
                antall = it["antall"].intValue()
            )
        )
    }
    return tilbehorListe
}

private fun elektriskRullestolInfo(hjelpemiddel: JsonNode): ElektriskRullestolInfo? {
    val elRullestolinfoJson = hjelpemiddel["elektriskRullestolInfo"] ?: return null
    return ElektriskRullestolInfo(
        godkjenningskurs = elRullestolinfoJson["godkjenningskurs"]?.booleanValue(),
        kanBetjeneManuellStyring = elRullestolinfoJson["kanBetjeneManuellStyring"]?.booleanValue(),
        kanBetjeneMotorisertStyring = elRullestolinfoJson["kanBetjeneMotorisertStyring"]?.booleanValue(),
        ferdesSikkertITrafikk = elRullestolinfoJson["ferdesSikkertITrafikk"]?.booleanValue(),
        nedsattGangfunksjon = elRullestolinfoJson["nedsattGangfunksjon"]?.booleanValue(),
        oppbevaringOgLagring = elRullestolinfoJson["oppbevaringOgLagring"]?.booleanValue(),
        oppbevaringInfo = elRullestolinfoJson["oppbevaringInfo"]?.textValue(),
        kjentMedForsikring = elRullestolinfoJson["kjentMedForsikring"]?.booleanValue(),
        harSpesialsykkel = elRullestolinfoJson["harSpesialsykkel"]?.booleanValue(),
        plasseringAvHendel = when (elRullestolinfoJson["plasseringAvHendel"]?.textValue()) {
            "Høyre" -> HendelPlassering.Høyre
            "Venstre" -> HendelPlassering.Venstre
            null -> null
            else -> throw RuntimeException("Ugyldig hendelplassering")
        },
        kabin = when (elRullestolinfoJson["kabin"]) {
            null -> null
            else -> Kabin(
                brukerOppfyllerKrav = elRullestolinfoJson["kabin"]["brukerOppfyllerKrav"].booleanValue(),
                kanIkkeAvhjelpesMedEnklereArsak = elRullestolinfoJson["kabin"]["kanIkkeAvhjelpesMedEnklereArsak"]?.textValue(),
                kanIkkeAvhjelpesMedEnklereBegrunnelse = elRullestolinfoJson["kabin"]["kanIkkeAvhjelpesMedEnklereBegrunnelse"]?.textValue(),
                arsakForBehovBegrunnelse = elRullestolinfoJson["kabin"]["arsakForBehovBegrunnelse"]?.textValue()
            )
        }
    )
}

private fun rullestolinfo(hjelpemiddel: JsonNode): RullestolInfo? {
    val rullestolInfoJson = hjelpemiddel["rullestolInfo"] ?: return null
    return RullestolInfo(
        skalBrukesIBil = rullestolInfoJson["skalBrukesIBil"]?.booleanValue(),
        sitteputeValg = when (rullestolInfoJson["sitteputeValg"]?.textValue()) {
            "TrengerSittepute" -> SitteputeValg.TrengerSittepute
            "StandardSittepute" -> SitteputeValg.StandardSittepute
            "LeggesTilSeparat" -> SitteputeValg.LeggesTilSeparat
            "HarFraFor" -> SitteputeValg.HarFraFor
            null -> null
            else -> throw RuntimeException("Ugyldig sitteputeValg")
        }
    )
}

private fun personlofterInfo(hjelpemiddel: JsonNode): PersonlofterInfo? {
    val personlofterInfoJson = hjelpemiddel["personlofterInfo"] ?: return null
    return PersonlofterInfo(harBehovForSeilEllerSele = personlofterInfoJson["harBehovForSeilEllerSele"].booleanValue())
}

private fun utlevertInfo(hjelpemiddel: JsonNode): UtlevertInfo? {
    val utlevertInfoJson = hjelpemiddel["utlevertInfo"] ?: return null
    return UtlevertInfo(
        overførtFraBruker = utlevertInfoJson["overførtFraBruker"]?.textValue(),
        annenKommentar = utlevertInfoJson["annenKommentar"]?.textValue(),
        utlevertType = when (utlevertInfoJson["utlevertType"]?.textValue()) {
            "FremskuttLager" -> UtlevertType.FremskuttLager
            "Korttidslån" -> UtlevertType.Korttidslån
            "Overført" -> UtlevertType.Overført
            "Annet" -> UtlevertType.Annet
            null -> null
            else -> throw java.lang.RuntimeException("ugyldig utlevertInfo")
        }
    )
}

private fun appInfo(hjelpemiddel: JsonNode): AppInfo? {
    val appInfoJson = hjelpemiddel["appInfo"] ?: return null
    return AppInfo(
        brukerHarProvdProvelisens = appInfoJson["brukerHarProvdProvelisens"].booleanValue(),
        stottepersonSkalAdministrere = appInfoJson["stottepersonSkalAdministrere"].booleanValue(),
        stottepersonHarProvdProvelisens = appInfoJson["stottepersonHarProvdProvelisens"]?.booleanValue(),
    )
}

class Søknadsdata(søknad: JsonNode, kommunenavn: String?) {
    val bruker = bruker(søknad)
    val formidler = formidler(søknad, kommunenavn)
    val hjelpemidler = hjelpemidler(søknad)
    val hjelpemiddelTotalAntall = søknad["soknad"]["hjelpemidler"]["hjelpemiddelTotaltAntall"].intValue()
    val oppfolgingsansvarlig = oppfolgingsansvarlig(søknad)
    val levering = levering(søknad)
}

class Bruker(
    val etternavn: String,
    val fnummer: String,
    val fornavn: String,
    val telefonNummer: String,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val boform: String,
    val bruksarena: Bruksarena,
    val funksjonsnedsettelser: List<Funksjonsnedsettelse>,
    val signatur: SignaturType,
    val kroppsmaal: Kroppsmaal?,
    val brukernummer: String?,
    val bekreftedeVilkår: List<BrukersituasjonVilkår>
)

enum class BrukersituasjonVilkår {
    NEDSATT_FUNKSJON, // Bruker har vesentlig og varig nedsatt funksjonsevne som følge av sykdom, skade eller lyte. Med varig menes 2 år eller livet ut.
    STORRE_BEHOV, // Hjelpemiddelet(ene) er nødvendig for å avhjelpe praktiske problemer i dagliglivet eller bli pleid i hjemmet. Brukers behov kan ikke løses med enklere og rimeligere hjelpemidler eller ved andre tiltak som ikke dekkes av NAV.
    PRAKTISKE_PROBLEM, // Hjelpemiddelet(ene) er egnet til å avhjelpe funksjonsnedsettelsen og bruker vil være i stand til å bruke det.
    PRAKTISKE_PROBLEMER_I_DAGLIGLIVET_V1, // Hjelpemiddelet er nødvendig for å avhjelpe praktiske problemer i dagliglivet, eller for å bli pleid i hjemmet.
    VESENTLIG_OG_VARIG_NEDSATT_FUNKSJONSEVNE_V1, // Bruker har vesentlig og varig nedsatt funksjonsevne som følge av sykdom, skade eller lyte. Med varig menes 2 år eller livet ut. Hjelpemiddelet skal ikke brukes til korttidsutlån eller til andre formål.
    KAN_IKKE_LOESES_MED_ENKLERE_HJELPEMIDLER_V1, // Innbyggers behov kan ikke løses med enklere og rimeligere hjelpemidler, eller ved andre tiltak som ikke dekkes av NAV.
    I_STAND_TIL_AA_BRUKE_HJELEPMIDLENE_V1, // Innbyggeren vil være i stand til å bruke hjelpemidlene. Jeg har ansvaret for at hjelpemidlene blir levert, og at nødvendig opplæring, tilpasning og montering blir gjort.
}

enum class SignaturType { BRUKER_BEKREFTER, FULLMAKT, FRITAK_FRA_FULLMAKT }
enum class Bruksarena { DAGLIGLIVET, UKJENT }
enum class Funksjonsnedsettelse { BEVEGELSE, HØRSEL, KOGNISJON }

data class Kroppsmaal(
    val setebredde: Int?,
    val laarlengde: Int?,
    val legglengde: Int?,
    val hoyde: Int?,
    val kroppsvekt: Int?,
)

class Formidler(
    val navn: String,
    val arbeidssted: String,
    val stilling: String,
    val adresse: String,
    val telefon: String,
    val treffesEnklest: String,
    val epost: String,
    val kommunenavn: String?,
)

class Oppfolgingsansvarlig(
    val navn: String,
    val arbeidssted: String,
    val stilling: String,
    val telefon: String,
    val ansvarFor: String,
)

class Hjelpemiddel(
    val antall: Int,
    val arsakForAntall: String?,
    val arsakForAntallBegrunnelse: String?,
    val beskrivelse: String,
    val hjelpemiddelkategori: String,
    val hmsNr: String,
    val tilleggsinformasjon: String,
    var rangering: String?,
    val utlevertFraHjelpemiddelsentralen: Boolean,
    val vilkarliste: List<HjelpemiddelVilkar>?,
    val tilbehorListe: List<Tilbehor>?,
    val begrunnelse: String?,
    val kanIkkeTilsvarande: Boolean,
    val navn: String?,
    val rullestolInfo: RullestolInfo?,
    val elektriskRullestolInfo: ElektriskRullestolInfo?,
    val personlofterInfo: PersonlofterInfo?,
    val utlevertInfo: UtlevertInfo?,
    val appInfo: AppInfo?,
)

data class RullestolInfo(
    val skalBrukesIBil: Boolean?,
    val sitteputeValg: SitteputeValg?,
)

data class AppInfo(
    val brukerHarProvdProvelisens: Boolean,
    val stottepersonSkalAdministrere: Boolean,
    val stottepersonHarProvdProvelisens: Boolean?,
)

data class PersonlofterInfo(
    val harBehovForSeilEllerSele: Boolean,
)

enum class SitteputeValg {
    TrengerSittepute, HarFraFor, StandardSittepute, LeggesTilSeparat
}

data class UtlevertInfo(
    val utlevertType: UtlevertType?,
    val overførtFraBruker: String?,
    val annenKommentar: String?,
)

enum class UtlevertType {
    FremskuttLager,
    Korttidslån,
    Overført,
    Annet
}

class ElektriskRullestolInfo(
    val godkjenningskurs: Boolean?,
    val kanBetjeneManuellStyring: Boolean?,
    val kanBetjeneMotorisertStyring: Boolean?,
    val ferdesSikkertITrafikk: Boolean?,
    val nedsattGangfunksjon: Boolean?,
    val oppbevaringOgLagring: Boolean?,
    val oppbevaringInfo: String?,
    val kjentMedForsikring: Boolean?,
    val harSpesialsykkel: Boolean?,
    val plasseringAvHendel: HendelPlassering?,
    val kabin: Kabin?
)

data class Kabin(
    val brukerOppfyllerKrav: Boolean,
    val kanIkkeAvhjelpesMedEnklereArsak: String?,
    val kanIkkeAvhjelpesMedEnklereBegrunnelse: String?,
    val arsakForBehovBegrunnelse: String?
)

enum class HendelPlassering {
    Høyre, Venstre
}

class Levering(
    val kontaktPerson: KontaktPerson,
    val leveringsmaate: Leveringsmaate?,
    val adresse: String?,
    val merknad: String?,
    val tilleggsinfo: List<LeveringTilleggsinfo>,
)

enum class LeveringTilleggsinfo {
    UTLEVERING_KALENDERAPP,
    ALLE_HJELPEMIDLER_ER_UTLEVERT,
}

class KontaktPerson(
    val navn: String? = null,
    val telefon: String? = null,
    val kontaktpersonType: KontaktpersonType,
)

enum class Leveringsmaate {
    FOLKEREGISTRERT_ADRESSE, ANNEN_ADRESSE, HJELPEMIDDELSENTRAL, ALLEREDE_LEVERT
}

enum class KontaktpersonType {
    HJELPEMIDDELBRUKER, HJELPEMIDDELFORMIDLER, ANNEN_KONTAKTPERSON, INGEN_KONTAKTPERSON
}

class HjelpemiddelVilkar(
    val vilkaarTekst: String,
    val tilleggsInfo: String?,
)

class Tilbehor(
    val hmsnr: String,
    val antall: Int?,
    val navn: String,
)

data class SøknadForBrukerOrdrelinje(
    val antall: Double,
    val antallEnhet: String,
    val kategori: String?,
    val artikkelBeskrivelse: String,
    val artikkelNr: String,
    val datoUtsendelse: String,

    // val artikkelBeskrivelse: String, <=> artikkelNavn
    // val serieNr: String?,

    var hmdbBeriket: Boolean = false,
    var hmdbProduktNavn: String? = null,
    var hmdbBeskrivelse: String? = null,
    var hmdbKategori: String? = null,
    var hmdbBilde: String? = null,
    var hmdbURL: String? = null,
) {
    fun berik(produkt: Produkt?): SøknadForBrukerOrdrelinje {
        if (produkt == null) {
            hmdbBeriket = false
            return this
        }
        hmdbBeriket = true
        hmdbProduktNavn = produkt.artikkelnavn
        hmdbBeskrivelse = produkt.produktbeskrivelse
        hmdbKategori = produkt.isotittel
        hmdbBilde = produkt.blobUrlLite
        if (produkt.produktId != null && produkt.artikkelId != null) {
            hmdbURL =
                "https://www.hjelpemiddeldatabasen.no/r11x.asp?linkinfo=${produkt.produktId}&art0=${produkt.artikkelId}&nart=1"
        }
        return this
    }
}
