package no.nav.hjelpemidler.soknad.modell

import java.util.UUID

data class Behovsmelding(
    val behovsmeldingType: BehovsmeldingType,
    val bestillingsordningsjekk: SoknadSjekkResultat? = null,
    val id: UUID,
    val soknad: Soknad? = null,
    val brukerpassbytte: BrukerpassBytteDTO? = null,
)

enum class BehovsmeldingType {
    SØKNAD,
    BESTILLING,
    BYTTE,
    BRUKERPASSBYTTE,
}

data class Soknad(
    val bruker: Bruker,
    val brukersituasjon: Brukersituasjon,
    val date: String?,
    val hjelpemidler: Hjelpemidler,
    val id: UUID,
    val levering: Levering,
    val innsender: Innsender,
)

data class Innsender(
    val godkjenningskurs: List<GodkjenningsKurs>?,
    val organisasjoner: List<Organisasjon>?,
    val somRolle: InnsenderRolle,
)

enum class InnsenderRolle {
    FORMIDLER,
    BESTILLER,
}

data class GodkjenningsKurs(
    val id: Int,
    val title: String,
    val kilde: String,
)

data class Bruker(
    val etternavn: String,
    val fnummer: Fødselsnummer,
    val fornavn: String,
    val signatur: Signaturtype,
    val telefonNummer: String,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val kilde: BrukerKilde?,
    val kroppsmaal: Kroppsmaal?,
    val erInformertOmRettigheter: Boolean?,
    val kommunenummer: String?,
    val brukernummer: String?,
)

enum class Signaturtype {
    BRUKER_BEKREFTER,
    FULLMAKT,
    FRITAK_FRA_FULLMAKT,
    IKKE_INNHENTET_FORDI_BYTTE,
    IKKE_INNHENTET_FORDI_BRUKERPASSBYTTE,
}

enum class BrukerKilde {
    PDL,
    FORMIDLER,
}

data class Kroppsmaal(
    val setebredde: Int?,
    val laarlengde: Int?,
    val legglengde: Int?,
    val hoyde: Int?,
    val kroppsvekt: Int?,
)

data class Brukersituasjon(
    val bekreftedeVilkår: List<BrukersituasjonVilkår>,
    val nedsattFunksjonTypes: NedsattFunksjonTypes,
)

enum class BrukersituasjonVilkår {
    PRAKTISKE_PROBLEMER_I_DAGLIGLIVET_V1,
    VESENTLIG_OG_VARIG_NEDSATT_FUNKSJONSEVNE_V1,
    KAN_IKKE_LOESES_MED_ENKLERE_HJELPEMIDLER_V1,
    I_STAND_TIL_AA_BRUKE_HJELEPMIDLENE_V1,
}

data class NedsattFunksjonTypes(
    val bevegelse: Boolean,
    val kognisjon: Boolean,
    val horsel: Boolean,
)

data class Hjelpemidler(
    val hjelpemiddelListe: List<HjelpemiddelItem>,
    val hjelpemiddelTotaltAntall: Int,
)

// The fields hmfFornavn and hmfEtternavn are not incl. as they come from PDL
data class Kontaktinfo(
    val hmfArbeidssted: String,
    val hmfEpost: String,
    val hmfPostadresse: String,
    val hmfPostnr: String,
    val hmfPoststed: String,
    val hmfStilling: String,
    val hmfTelefon: String,
    val hmfTreffesEnklest: String,
)

data class Levering(
    val hmfArbeidssted: String,
    val hmfEpost: String,
    val hmfEtternavn: String,
    val hmfFornavn: String,
    val hmfPostadresse: String,
    val hmfPostnr: String,
    val hmfPoststed: String,
    val hmfStilling: String,
    val hmfTelefon: String,
    val hmfTreffesEnklest: String,
    val merknadTilUtlevering: String,
    val opfAnsvarFor: String?,
    val opfArbeidssted: String?,
    val opfEtternavn: String?,
    val opfFornavn: String?,
    val opfRadioButton: Oppfoelger,
    val opfStilling: String?,
    val opfTelefon: String?,
    val utleveringEtternavn: String?,
    val utleveringFornavn: String?,
    val utleveringPostadresse: String?,
    val utleveringPostnr: String?,
    val utleveringPoststed: String?,
    val utleveringTelefon: String?,
    val utleveringskontaktpersonRadioButton: Kontaktperson?,
    val utleveringsmaateRadioButton: UtleveringsMaate?,
    val tilleggsinfo: List<LeveringTilleggsinfo> = emptyList(),
)

enum class LeveringTilleggsinfo {
    UTLEVERING_KALENDERAPP,
    ALLE_HJELPEMIDLER_ER_UTLEVERT,
}

enum class Oppfoelger {
    Hjelpemiddelformidler,
    NoenAndre,
}

enum class Kontaktperson {
    Hjelpemiddelbruker,
    Hjelpemiddelformidler,
    AnnenKontaktperson,
}

enum class UtleveringsMaate {
    FolkeregistrertAdresse,
    AnnenBruksadresse,
    Hjelpemiddelsentralen,
    AlleredeUtlevertAvNav, // Deprecated
}

data class HjelpemiddelItem(
    val antall: Int,
    val arsakForAntall: String? = null,
    val arsakForAntallBegrunnelse: String? = null,
    val beskrivelse: String,
    val hjelpemiddelkategori: String,
    val hmsNr: String,
    val tilleggsinformasjon: String,
    val uniqueKey: String,
    val utlevertFraHjelpemiddelsentralen: Boolean,
    val vilkaroverskrift: String? = null,
    val vilkarliste: List<HjelpemiddelVilkar>? = null,
    val tilbehorListe: List<Tilbehor>? = null,
    val begrunnelsen: String? = null,
    val kanIkkeTilsvarande: String? = null,
    val navn: String? = null,
    val produkt: HjelpemiddelProdukt? = null,
    val rullestolInfo: RullestolInfo? = null,
    val utlevertInfo: UtlevertInfo? = null,
    val personlofterInfo: PersonlofterInfo? = null,
    val elektriskRullestolInfo: ElektriskRullestolInfo? = null,
    val appInfo: AppInfo? = null,
    val varmehjelpemiddelInfo: VarmehjelpemiddelInfo? = null,
    val sengeInfo: SengeInfo? = null,
    val elektriskVendesystemInfo: ElektriskVendesystemInfo? = null,
    val ganghjelpemiddelInfo: GanghjelpemiddelInfo? = null,
    val posisjoneringssystemInfo: PosisjoneringssystemInfo? = null,
    val posisjoneringsputeForBarnInfo: PosisjoneringsputeForBarnInfo? = null,
    val oppreisningsStolInfo: OppreisningsStolInfo? = null,
    val diverseInfo: DiverseInfo? = null,
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
    SKRÅLØFT,
    RETTLØFT,
}

enum class OppreisningsStolBruksområde {
    EGEN_BOENHET,
    FELLESAREAL,
}

enum class OppreisningsStolBehov {
    OPPGAVER_I_DAGLIGLIVET,
    PLEID_I_HJEMMET,
    FLYTTE_MELLOM_STOL_OG_RULLESTOL,
}

enum class SideBetjeningsPanelPosisjon {
    HØYRE,
    VENSTRE,
}

data class DiverseInfo(
    val takhoydeStottestangCm: Int? = null,
    val sitteputeSkalBrukesIRullestolFraNav: Boolean? = null,
)

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

enum class PosisjoneringsputeOppgaverIDagligliv {
    SPISE_DRIKKE_OL,
    BRUKE_DATAUTSTYR,
    FØLGE_OPP_BARN,
    HOBBY_FRITID_U26,
    ANNET,
}

enum class BruksområdeGanghjelpemiddel {
    TIL_FORFLYTNING,
    TIL_TRENING_OG_ANNET,
}

enum class GanghjelpemiddelType {
    GÅBORD,
    SPARKESYKKEL,
    KRYKKE,
    GÅTRENING,
    GÅSTOL,
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

data class AppInfo(
    val brukerHarProvdProvelisens: Boolean,
    val stottepersonSkalAdministrere: Boolean,
    val stottepersonHarProvdProvelisens: Boolean?,
)

data class VarmehjelpemiddelInfo(
    val harHelseopplysningerFraFør: Boolean?,
    val legeBekrefterDiagnose: Boolean?,
    val opplysningerFraLegeOppbevaresIKommune: Boolean?,
)

data class ElektriskRullestolInfo(
    val godkjenningskurs: Boolean?,
    val kanBetjeneManuellStyring: Boolean?,
    val kanBetjeneMotorisertStyring: Boolean?,
    val ferdesSikkertITrafikk: Boolean?,
    val nedsattGangfunksjon: Boolean?,
    val oppbevaringOgLagring: Boolean?,
    val oppbevaringInfo: String?,
    val kjentMedForsikring: Boolean?,
    val harSpesialsykkel: Boolean?,
    val plasseringAvHendel: PlasseringType?,
    val kabin: Kabin?,
)

data class Kabin(
    val brukerOppfyllerKrav: Boolean,
    val kanIkkeAvhjelpesMedEnklereArsak: String?,
    val kanIkkeAvhjelpesMedEnklereBegrunnelse: String?,
    val arsakForBehovBegrunnelse: String?,
)

data class PersonlofterInfo(
    val harBehovForSeilEllerSele: Boolean,
)

data class RullestolInfo(
    val skalBrukesIBil: Boolean?,
    val sitteputeValg: SitteputeValg?,
)

data class UtlevertInfo(
    val utlevertType: UtlevertType?,
    val overførtFraBruker: String?,
    val annenKommentar: String?,
)

enum class PlasseringType {
    Venstre,
    Høyre,
}

enum class UtlevertType {
    FremskuttLager,
    Korttidslån,
    Overført,
    Annet,
}

enum class SitteputeValg {
    TrengerSittepute,
    HarFraFor,
}

enum class MadrassValg {
    TrengerMadrass,
    HarFraFor,
}

data class HøyGrindValg(
    val erKjentMedTvangsAspekt: Boolean,
    val harForsøktOpptrening: Boolean,
    val harIkkeForsøktOpptreningBegrunnelse: String?,
    val erLagetPlanForOppfølging: Boolean,
)

data class Tilbehor(
    val hmsnr: String,
    val antall: Int?,
    val navn: String,
    val automatiskGenerert: AutomatiskGenerertTilbehor?,
    val brukAvForslagsmotoren: BrukAvForslagsmotoren?,
)

enum class AutomatiskGenerertTilbehor {
    Sittepute,
}

data class BrukAvForslagsmotoren(
    val lagtTilFraForslagsmotoren: Boolean,
    val oppslagAvNavn: Boolean,
)

data class HjelpemiddelVilkar(
    val vilkartekst: String,
    val checked: Boolean,
    val kreverTilleggsinfo: Boolean?,
    val tilleggsinfo: String?,
)
