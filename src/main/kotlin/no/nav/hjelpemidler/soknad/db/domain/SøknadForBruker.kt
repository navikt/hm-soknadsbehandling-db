package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
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

    var bekreftedeVilkår: List<BrukersituasjonVilkår> =
        brukerSituasjonNode["bekreftedeVilkår"]?.let { bekreftedeVilkårReader.readValue(it) }
            ?: emptyList()
    if (bekreftedeVilkår.isEmpty()) {
        bekreftedeVilkår = mutableListOf<BrukersituasjonVilkår>().apply {
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
    }

    return Bruker(
        fnummer = brukerNode["fnummer"].textValue(),
        fornavn = brukerNode["fornavn"].textValue(),
        etternavn = brukerNode["etternavn"].textValue(),
        telefonNummer = brukerNode["telefonNummer"].textValue(),
        adresse = brukerNode["adresse"]?.textValue(),
        postnummer = brukerNode["postnummer"]?.textValue(),
        poststed = brukerNode["poststed"]?.textValue(),
        boform = brukerSituasjonNode["bostedRadioButton"]?.textValue(),
        bruksarena = bruksarenaBruker(søknad["soknad"]["brukersituasjon"]),
        funksjonsnedsettelser = funksjonsnedsettelser(søknad),
        signatur = signaturType(søknad),
        kroppsmaal = kroppsmaal(brukerNode),
        brukernummer = brukerNode["brukernummer"]?.textValue(),
        bekreftedeVilkår = bekreftedeVilkår,
    )
}

fun bruksarenaBruker(brukersituasjon: JsonNode): BruksarenaBruker {
    return when (brukersituasjon["bruksarenaErDagliglivet"]?.booleanValue()) {
        true -> BruksarenaBruker.DAGLIGLIVET
        else -> BruksarenaBruker.UKJENT
    }
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

private fun signaturType(søknad: JsonNode): SignaturType =
    objectMapper.treeToValue(søknad["soknad"]["bruker"]["signatur"])

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
            varmehjelpemiddelInfo = varmehjelpemiddelInfo(it),
            sengeInfo = sengeInfo(it),
            elektriskVendesystemInfo = elektriskVendesystemInfo(it),
            ganghjelpemiddelInfo = ganghjelpemiddelInfo(it),
            posisjoneringssystemInfo = posisjoneringssystemInfo(it),
            posisjoneringsputeForBarnInfo = posisjoneringsputeForBarnInfo(it),
            oppreisningsStolInfo = oppreisningsStolInfo(it),
            diverseInfo = diverseInfo(it),
            bytter = bytter(it),
            bruksarena = bruksarena(it)
        )
        hjelpemidler.add(hjelpemiddel)
    }
    return hjelpemidler
}

private fun oppreisningsStolInfo(hjelpemiddel: JsonNode): OppreisningsStolInfo? {
    val oppreisningsStolInfo = hjelpemiddel["oppreisningsStolInfo"] ?: return null
    return objectMapper.treeToValue<OppreisningsStolInfo>(oppreisningsStolInfo)
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
            "PUTENE_SKAL_KOMBINERES_I_POSISJONERING" -> "PUTENE_SKAL_KOMBINERES_I_POSISJONERING"
            "BEHOV_HJEMME_OG_I_BARNEHAGE" -> "BEHOV_HJEMME_OG_I_BARNEHAGE"
            "PUTENE_SKAL_SETTES_SAMMEN_VED_BRUK" -> "PUTENE_SKAL_SETTES_SAMMEN_VED_BRUK"
            else -> "UKJENT_ÅRSAK"
        }
    }

    return arsak
}

private fun vilkaar(hjelpemiddel: JsonNode): List<HjelpemiddelVilkar> {
    val vilkarListe = mutableListOf<HjelpemiddelVilkar>()
    hjelpemiddel["vilkarliste"]?.filter { it["checked"].asBoolean() }?.forEach {
        vilkarListe.add(
            HjelpemiddelVilkar(
                vilkaarTekst = it["vilkartekst"].textValue(),
                tilleggsinfo = it["tilleggsinfo"]?.textValue()
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
            else -> throw IllegalArgumentException("Ugyldig hendelplassering")
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

private fun varmehjelpemiddelInfo(hjelpemiddel: JsonNode): VarmehjelpemiddelInfo? {
    val varmehjelpemiddelInfoJson = hjelpemiddel["varmehjelpemiddelInfo"] ?: return null
    return VarmehjelpemiddelInfo(
        harHelseopplysningerFraFør = varmehjelpemiddelInfoJson["harHelseopplysningerFraFør"]?.booleanValue(),
        legeBekrefterDiagnose = varmehjelpemiddelInfoJson["legeBekrefterDiagnose"]?.booleanValue(),
        opplysningerFraLegeOppbevaresIKommune = varmehjelpemiddelInfoJson["opplysningerFraLegeOppbevaresIKommune"]?.booleanValue()
    )
}

private fun sengeInfo(hjelpemiddel: JsonNode): SengeInfo? {
    val sengeInfoJson = hjelpemiddel["sengeInfo"] ?: return null
    val høyGrindValg = sengeInfoJson["høyGrindValg"] ?: null
    return SengeInfo(
        påkrevdBehov = sengeInfoJson["påkrevdBehov"]?.textValue(),
        brukerOppfyllerPåkrevdBehov = sengeInfoJson["brukerOppfyllerPåkrevdBehov"]?.booleanValue(),
        behovForSeng = sengeInfoJson["behovForSeng"]?.textValue(),
        behovForSengBegrunnelse = sengeInfoJson["behovForSengBegrunnelse"]?.textValue(),
        madrassValg = when (sengeInfoJson["madrassValg"]?.textValue()) {
            "TrengerMadrass" -> MadrassValg.TrengerMadrass
            "HarFraFor" -> MadrassValg.HarFraFor
            null -> null
            else -> throw RuntimeException("Ugyldig sitteputeValg")
        },
        høyGrindValg = høyGrindValg?.let { objectMapper.treeToValue<HøyGrindValg>(it) }
    )
}

private fun elektriskVendesystemInfo(hjelpemiddel: JsonNode): ElektriskVendesystemInfo? {
    val elektriskVendesystemInfoJson = hjelpemiddel["elektriskVendesystemInfo"] ?: return null
    return ElektriskVendesystemInfo(
        sengForMontering = sengForMontering(elektriskVendesystemInfoJson),
        standardLakenByttesTilRiktigStørrelseAvNav = elektriskVendesystemInfoJson["standardLakenByttesTilRiktigStørrelseAvNav"]?.booleanValue(),
    )
}

private fun ganghjelpemiddelInfoBruksområde(value: String?): BruksområdeGanghjelpemiddel? {
    return when (value) {
        "TIL_FORFLYTNING" -> BruksområdeGanghjelpemiddel.TIL_FORFLYTNING
        "TIL_TRENING_OG_ANNET" -> BruksområdeGanghjelpemiddel.TIL_TRENING_OG_ANNET
        null -> null
        else -> throw IllegalArgumentException("Ukjent enum verdi '$value'")
    }
}

private fun ganghjelpemiddelInfoType(value: String?): GanghjelpemiddelType? {
    return when (value) {
        "GÅBORD" -> GanghjelpemiddelType.GÅBORD
        "SPARKESYKKEL" -> GanghjelpemiddelType.SPARKESYKKEL
        "KRYKKE" -> GanghjelpemiddelType.KRYKKE
        "GÅTRENING" -> GanghjelpemiddelType.GÅTRENING
        "GÅSTOL" -> GanghjelpemiddelType.GÅSTOL
        null -> null
        else -> throw IllegalArgumentException("Ukjent enum verdi '$value'")
    }
}

private fun ganghjelpemiddelInfo(hjelpemiddel: JsonNode): GanghjelpemiddelInfo? {
    val ganghjelpemiddelInfoJson = hjelpemiddel["ganghjelpemiddelInfo"] ?: return null
    return GanghjelpemiddelInfo(
        brukerErFylt26År = ganghjelpemiddelInfoJson["brukerErFylt26År"]?.booleanValue(),
        hovedformålErForflytning = ganghjelpemiddelInfoJson["hovedformålErForflytning"]?.booleanValue(),
        kanIkkeBrukeMindreAvansertGanghjelpemiddel = ganghjelpemiddelInfoJson["kanIkkeBrukeMindreAvansertGanghjelpemiddel"]?.booleanValue(),
        type = ganghjelpemiddelInfoType(ganghjelpemiddelInfoJson["type"]?.textValue()),
        bruksområde = ganghjelpemiddelInfoBruksområde(ganghjelpemiddelInfoJson["bruksområde"]?.textValue()),
        detErLagetEnMålrettetPlan = ganghjelpemiddelInfoJson["detErLagetEnMålrettetPlan"]?.booleanValue(),
        planenOppbevaresIKommunen = ganghjelpemiddelInfoJson["planenOppbevaresIKommunen"]?.booleanValue(),
    )
}

private fun sengForMontering(hjelpemiddel: JsonNode): SengForVendesystemMontering? {
    val sengForMonteringJson = hjelpemiddel["sengForMontering"] ?: return null
    return SengForVendesystemMontering(
        hmsnr = sengForMonteringJson["hmsnr"]?.textValue(),
        navn = sengForMonteringJson["navn"]?.textValue(),
        madrassbredde = sengForMonteringJson["madrassbredde"]?.intValue()
    )
}

private val posisjoneringsputeOppgaverIDagliglivReader =
    objectMapper.readerFor(object : TypeReference<List<PosisjoneringsputeOppgaverIDagligliv>?>() {})

private fun posisjoneringssystemInfo(hjelpemiddel: JsonNode): PosisjoneringssystemInfo? {
    val posisjoneringssystemInfoJson = hjelpemiddel["posisjoneringssystemInfo"] ?: return null
    return PosisjoneringssystemInfo(
        skalIkkeBrukesSomBehandlingshjelpemiddel = posisjoneringssystemInfoJson["skalIkkeBrukesSomBehandlingshjelpemiddel"]?.booleanValue(),
        skalIkkeBrukesTilRenSmertelindring = posisjoneringssystemInfoJson["skalIkkeBrukesTilRenSmertelindring"]?.booleanValue(),
        behov = posisjoneringsputeBehov(posisjoneringssystemInfoJson["behov"]?.textValue()),
        oppgaverIDagliglivet = posisjoneringssystemInfoJson["oppgaverIDagliglivet"]?.let {
            posisjoneringsputeOppgaverIDagliglivReader.readValue(
                it
            )
        } ?: emptyList(),
        oppgaverIDagliglivetAnnet = posisjoneringssystemInfoJson["oppgaverIDagliglivetAnnet"]?.textValue()
    )
}

private fun posisjoneringsputeForBarnInfo(hjelpemiddel: JsonNode): PosisjoneringsputeForBarnInfo? {
    val posisjoneringsputeForBarnInfoJson = hjelpemiddel["posisjoneringsputeForBarnInfo"] ?: return null
    return PosisjoneringsputeForBarnInfo(
        bruksområde = posisjoneringsputeForBarnBruk(posisjoneringsputeForBarnInfoJson["bruksområde"]?.textValue()),
        brukerErOver26År = posisjoneringsputeForBarnInfoJson["brukerErOver26År"]?.booleanValue(),
        detErLagetEnMålrettetPlan = posisjoneringsputeForBarnInfoJson["detErLagetEnMålrettetPlan"]?.booleanValue(),
        planenOppbevaresIKommunen = posisjoneringsputeForBarnInfoJson["planenOppbevaresIKommunen"]?.booleanValue(),
    )
}

private fun bruksarena(hjelpemiddel: JsonNode): List<Bruksarena> {
    val bruksarenaJson = hjelpemiddel["bruksarena"] ?: return emptyList()
    return objectMapper.treeToValue(bruksarenaJson)
}

private fun diverseInfo(hjelpemiddel: JsonNode): Map<String, String> {
    val diverseInfoJson = hjelpemiddel["diverseInfo"] ?: return emptyMap()
    return objectMapper.treeToValue(diverseInfoJson)
}

private fun bytter(hjelpemiddel: JsonNode): List<Bytte> {
    val diverseInfoJson = hjelpemiddel["bytter"] ?: return emptyList()
    return objectMapper.treeToValue(diverseInfoJson)
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
    val boform: String?,
    val bruksarena: BruksarenaBruker,
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

enum class SignaturType { BRUKER_BEKREFTER, FULLMAKT, FRITAK_FRA_FULLMAKT, IKKE_INNHENTET_FORDI_BYTTE }
enum class BruksarenaBruker { DAGLIGLIVET, UKJENT }
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
    val varmehjelpemiddelInfo: VarmehjelpemiddelInfo?,
    val sengeInfo: SengeInfo?,
    val elektriskVendesystemInfo: ElektriskVendesystemInfo?,
    val ganghjelpemiddelInfo: GanghjelpemiddelInfo?,
    val posisjoneringssystemInfo: PosisjoneringssystemInfo?,
    val posisjoneringsputeForBarnInfo: PosisjoneringsputeForBarnInfo?,
    val oppreisningsStolInfo: OppreisningsStolInfo?,
    val diverseInfo: Map<String, String> = emptyMap(),
    val bytter: List<Bytte> = emptyList(),
    val bruksarena: List<Bruksarena>? = null, // TODO Kan fjerne nullable når ny rammeavtale gangehjelpemidler er lansert (etter 2. jan 2023)
)

enum class Bruksarena {
    EGET_HJEM,
    EGET_HJEM_IKKE_AVLASTNING,
    OMSORGSBOLIG_BOFELLESKAP_SERVICEBOLIG,
    BARNEHAGE,
    GRUNN_ELLER_VIDEREGÅENDESKOLE,
    SKOLEFRITIDSORDNING,
    INSTITUSJON,
    INSTITUSJON_BARNEBOLIG,
    INSTITUSJON_BARNEBOLIG_IKKE_PERSONLIG_BRUK,
}

data class Bytte(
    val erTilsvarende: Boolean,
    val hmsnr: String,
    val serienr: String? = null,
    val hjmNavn: String,
    val hjmKategori: String,
    val årsak: BytteÅrsak? = null,
)

enum class BytteÅrsak {
    UTSLITT,
    VOKST_FRA,
    ENDRINGER_I_INNBYGGERS_FUNKSJON,
    FEIL_STØRRELSE,
    VURDERT_SOM_ØDELAGT_AV_LOKAL_TEKNIKER,
}

data class PosisjoneringsputeForBarnInfo(
    val bruksområde: PosisjoneringsputeForBarnBruk?,
    val brukerErOver26År: Boolean?,
    val detErLagetEnMålrettetPlan: Boolean?,
    val planenOppbevaresIKommunen: Boolean?,
)

enum class PosisjoneringsputeForBarnBruk {
    TILRETTELEGGE_UTGANGSSTILLING,
    TRENING_AKTIVITET_STIMULERING,
}

private fun posisjoneringsputeForBarnBruk(value: String?): PosisjoneringsputeForBarnBruk? {
    return when (value) {
        "TILRETTELEGGE_UTGANGSSTILLING" -> PosisjoneringsputeForBarnBruk.TILRETTELEGGE_UTGANGSSTILLING
        "TRENING_AKTIVITET_STIMULERING" -> PosisjoneringsputeForBarnBruk.TRENING_AKTIVITET_STIMULERING
        null -> null
        else -> throw IllegalArgumentException("Ukjent enum verdi '$value'")
    }
}

data class PosisjoneringssystemInfo(
    val skalIkkeBrukesSomBehandlingshjelpemiddel: Boolean?,
    val skalIkkeBrukesTilRenSmertelindring: Boolean?,
    val behov: PosisjoneringsputeBehov?,
    val oppgaverIDagliglivet: List<PosisjoneringsputeOppgaverIDagligliv>?,
    val oppgaverIDagliglivetAnnet: String?,
)

enum class PosisjoneringsputeBehov {
    STORE_LAMMELSER,
    DIREKTE_AVHJELPE_I_DAGLIGLIVET,
}

private fun posisjoneringsputeBehov(value: String?): PosisjoneringsputeBehov? {
    return when (value) {
        "STORE_LAMMELSER" -> PosisjoneringsputeBehov.STORE_LAMMELSER
        "DIREKTE_AVHJELPE_I_DAGLIGLIVET" -> PosisjoneringsputeBehov.DIREKTE_AVHJELPE_I_DAGLIGLIVET
        null -> null
        else -> throw IllegalArgumentException("Ukjent enum verdi '$value'")
    }
}

enum class PosisjoneringsputeOppgaverIDagligliv {
    SPISE_DRIKKE_OL,
    BRUKE_DATAUTSTYR,
    FØLGE_OPP_BARN,
    HOBBY_FRITID_U26,
    ANNET,
}

enum class BruksområdeGanghjelpemiddel {
    TIL_FORFLYTNING,
    TIL_TRENING_OG_ANNET
}

enum class GanghjelpemiddelType {
    GÅBORD,
    SPARKESYKKEL,
    KRYKKE,
    GÅTRENING,
    GÅSTOL
}

data class GanghjelpemiddelInfo(
    val brukerErFylt26År: Boolean?,
    val hovedformålErForflytning: Boolean?,
    val kanIkkeBrukeMindreAvansertGanghjelpemiddel: Boolean?,
    val type: GanghjelpemiddelType?,
    val bruksområde: BruksområdeGanghjelpemiddel?,
    val detErLagetEnMålrettetPlan: Boolean?,
    val planenOppbevaresIKommunen: Boolean?,
)

data class ElektriskVendesystemInfo(
    val sengForMontering: SengForVendesystemMontering?,
    val standardLakenByttesTilRiktigStørrelseAvNav: Boolean?,
)

data class SengForVendesystemMontering(
    val hmsnr: String?,
    val navn: String?,
    val madrassbredde: Int?,
)

data class SengeInfo(
    val påkrevdBehov: String?,
    val brukerOppfyllerPåkrevdBehov: Boolean?,
    val behovForSeng: String?,
    val behovForSengBegrunnelse: String?,
    val madrassValg: MadrassValg?,
    val høyGrindValg: HøyGrindValg?,
)

data class VarmehjelpemiddelInfo(
    val harHelseopplysningerFraFør: Boolean?,
    val legeBekrefterDiagnose: Boolean?,
    val opplysningerFraLegeOppbevaresIKommune: Boolean?
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

enum class MadrassValg {
    TrengerMadrass, HarFraFor
}

data class HøyGrindValg(
    val erKjentMedTvangsAspekt: Boolean,
    val harForsøktOpptrening: Boolean,
    val harIkkeForsøktOpptreningBegrunnelse: String?,
    val erLagetPlanForOppfølging: Boolean,
)

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
    val tilleggsinfo: String?,
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
        // TODO: FIXME: Hent fra graphql i stede for å generere her
        if (produkt.produktId != null && produkt.artikkelId != null) {
            hmdbURL =
                "https://www.hjelpemiddeldatabasen.no/r11x.asp?linkinfo=${produkt.produktId}&art0=${produkt.artikkelId}&nart=1"
        }
        return this
    }
}

data class OppreisningsStolInfo(
    val kanBrukerReiseSegSelvFraVanligStol: Boolean,
    val behov: List<OppreisningsStolBehov>?,
    val behovForStolBegrunnelse: String?,
    val sideBetjeningsPanel: SideBetjeningsPanelPosisjon?,
    val bruksområde: OppreisningsStolBruksområde?,
    val annetTrekkKanBenyttes: Boolean,
    val løftType: OppreisningsStolLøftType,
)

enum class OppreisningsStolLøftType {
    SKRÅLØFT, RETTLØFT
}

enum class OppreisningsStolBruksområde {
    EGEN_BOENHET, FELLESAREAL
}

enum class OppreisningsStolBehov {
    OPPGAVER_I_DAGLIGLIVET,
    PLEID_I_HJEMMET,
    FLYTTE_MELLOM_STOL_OG_RULLESTOL,
}

enum class SideBetjeningsPanelPosisjon {
    HØYRE, VENSTRE
}
