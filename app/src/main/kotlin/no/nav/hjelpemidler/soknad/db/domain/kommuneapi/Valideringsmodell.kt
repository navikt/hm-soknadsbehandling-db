package no.nav.hjelpemidler.soknad.db.domain.kommuneapi

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hjelpemidler.behovsmeldingsmodell.FritakFraBegrunnelseÅrsak
import no.nav.hjelpemidler.behovsmeldingsmodell.LeveringTilleggsinfo
import no.nav.hjelpemidler.behovsmeldingsmodell.ÅrsakForAntall
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.soknad.db.rolle.Næringskode
import java.util.UUID

/**
 * Merk merk merk: Ikke gjør endringer uten å lese dette:
 *
 * Denne datamodellen brukes til å validere at noen har vurdert juridisk hva som kan deles med kommunen over
 * kommune-apiet ettersom søknaden utvikler seg. Hvis endringer gjøres i hm-soknad / hm-soknad-api som ender opp i
 * data-feltet i søknadsdatabasen, så vil valideringen her feile og kommune-apiet vil ikke lengre klare å hente
 * kvitteringer før noen har oppdatert datamodellen her til å reflektere endringen, samt oppdatert
 * "fun filtrerForKommuneApiet()" slik at den filtrerer ut nye verdier som ikke kan deles.
 *
 * I utgangspunktet skal alt som er generert av innsender være med. Men vi filtrerer ut feks. bestillingsordningsjekk og
 * soknad->innsender (godkjenningskurs, organisasjoner, osv.).
 *
 * Fremgangsmåte for å fikse en feilende validering:
 *       Utvid datamodellen med de nye feltene. Vurder om de kan kvitteres tilbake til kommmunen. Hvis ikke gjør
 *      du feltet nullable og endrer `fun filtrerForKommuneApiet()` under slik at feltet filtreres ut før data sendes til
 *      kommunen.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Behovsmelding(
    val behovsmeldingType: BehovsmeldingType,
    val bestillingsordningsjekk: JsonNode?,
    val soknad: Soknad,
    val id: UUID,
) {
    fun filtrerForKommuneApiet() = this.copy(
        bestillingsordningsjekk = null,
        soknad = this.soknad.copy(innsender = null),
    )

    companion object {
        private val specializedObjectMapper: JsonMapper = jacksonMapperBuilder()
            .addModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Skal feile hvis man har ukjente verdier i JsonNode, da må denne vedlikeholdes og hva som deles med
            // kommunen revurderes!
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION, !Environment.current.tier.isProd)
            .build()

        fun fraJsonNode(node: JsonNode): Behovsmelding {
            return runCatching {
                specializedObjectMapper.readValue<Behovsmelding>(node.toString())
            }.getOrElse { cause ->
                throw RuntimeException("Kunne ikke opprette Behovsmelding-typen fra JsonNode: ${cause.message}", cause)
            }
        }
    }
}

enum class BehovsmeldingType {
    SØKNAD,
    BESTILLING,
    BYTTE,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Soknad(
    val bruker: Bruker,
    val brukersituasjon: Brukersituasjon,
    val date: String?,
    val hjelpemidler: Hjelpemidler,
    val id: UUID,
    val levering: Levering,
    var innsender: Innsender?,
    val hast: Hast? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Innsender(
    val godkjenningskurs: List<GodkjenningsKurs>?,
    val organisasjoner: List<Organisasjon>?,
    val somRolle: InnsenderRolle,
    val erKommunaltAnsatt: Boolean?,
    val tjenestligeBehovForUtlånsoversikt: Set<String> = emptySet(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Organisasjon(
    val orgnr: String,
    val navn: String,
    val orgform: String = "",
    val overordnetOrgnr: String? = null,
    val næringskoder: List<Næringskode> = emptyList(),
    val kommunenummer: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Næringskode(
    val kode: String,
    val beskrivelse: String = "",
)

enum class InnsenderRolle {
    FORMIDLER,
    BESTILLER,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GodkjenningsKurs(
    val id: Int,
    val title: String,
    val kilde: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    val borIPilotkommuneForHast: Boolean? = false,
    val borIPilotkommuneForFunksjonsbeskrivelse: Boolean? = false,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Fødselsnummer(@get:JsonValue val value: String) {

    init {
        if (!elevenDigitsPattern.matches(value)) {
            throw IllegalArgumentException("$value is not a valid fnr")
        }
    }

    companion object {
        private val elevenDigitsPattern = Regex("\\d{11}")
    }
}

enum class Signaturtype {
    BRUKER_BEKREFTER,
    FULLMAKT,
    FRITAK_FRA_FULLMAKT,
    IKKE_INNHENTET_FORDI_BYTTE,
}

enum class BrukerKilde {
    PDL,
    FORMIDLER,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Kroppsmaal(
    val setebredde: Int?,
    val laarlengde: Int?,
    val legglengde: Int?,
    val hoyde: Int?,
    val kroppsvekt: Int?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Brukersituasjon(
    val bostedRadioButton: String?,
    val bruksarenaErDagliglivet: Boolean,
    val nedsattFunksjon: Boolean,
    val nedsattFunksjonTypes: NedsattFunksjonTypes?,
    val storreBehov: Boolean,
    val praktiskeProblem: Boolean,
    val skalIkkeBrukesTilAndreFormaal: Boolean?, // Kun for bestiller
    val bekreftedeVilkår: List<BrukersituasjonVilkår>?,
    val funksjonsbeskrivelse: Funksjonsbeskrivelse?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NedsattFunksjonTypes(
    val bevegelse: Boolean,
    val kognisjon: Boolean,
    val horsel: Boolean,
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

data class Funksjonsbeskrivelse(
    val innbyggersVarigeFunksjonsnedsettelse: InnbyggersVarigeFunksjonsnedsettelse,
    val diagnose: String?,
    val beskrivelse: String,
)

enum class InnbyggersVarigeFunksjonsnedsettelse {
    ALDERDOMSSVEKKELSE,
    ANNEN_VARIG_DIAGNOSE,
    UAVKLART,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Hjelpemidler(
    val hjelpemiddelListe: List<HjelpemiddelItem>,
    val hjelpemiddelTotaltAntall: Int,
)

// The fields hmfFornavn and hmfEtternavn are not incl. as they come from PDL
@JsonInclude(JsonInclude.Include.NON_NULL)
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

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    val hjelpemiddelformidlerKommunenavn: String?,
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
    AlleredeUtlevertAvNav,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HjelpemiddelItem(
    val antall: Int,
    val arsakForAntall: String?,
    val arsakForAntallBegrunnelse: String?,
    val beskrivelse: String,
    val hjelpemiddelkategori: String,
    val hmsNr: String,
    val tilleggsinformasjon: String,
    val uniqueKey: String,
    val utlevertFraHjelpemiddelsentralen: Boolean,
    val vilkaroverskrift: String?,
    val vilkarliste: List<HjelpemiddelVilkar>?,
    val tilbehorListe: List<Tilbehor>?,
    val begrunnelsen: String?,
    val kanIkkeTilsvarande: String?,
    val navn: String?,
    val produkt: HjelpemiddelProdukt?,
    val rullestolInfo: RullestolInfo?,
    val utlevertInfo: UtlevertInfo?,
    val personlofterInfo: PersonlofterInfo?,
    val elektriskRullestolInfo: ElektriskRullestolInfo?,
    val appInfo: AppInfo?,
    val varmehjelpemiddelInfo: VarmehjelpemiddelInfo?,
    val sengeInfo: SengeInfo?,
    val elektriskVendesystemInfo: ElektriskVendesystemInfo?,
    val ganghjelpemiddelInfo: GanghjelpemiddelInfo? = null,
    val posisjoneringssystemInfo: PosisjoneringssystemInfo?,
    val posisjoneringsputeForBarnInfo: PosisjoneringsputeForBarnInfo?,
    val oppreisningsStolInfo: OppreisningsStolInfo?,
    val diverseInfo: Map<String, String> = emptyMap(),
    val bytter: List<Bytte> = emptyList(),
    val bruksarena: List<Bruksarena>? = null,
    val årsakForAntallEnum: ÅrsakForAntall?,
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

@JsonInclude(JsonInclude.Include.NON_NULL)
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

@JsonInclude(JsonInclude.Include.NON_NULL)
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

@JsonInclude(JsonInclude.Include.NON_NULL)
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GanghjelpemiddelInfo(
    val brukerErFylt26År: Boolean?,
    val hovedformålErForflytning: Boolean?,
    val kanIkkeBrukeMindreAvansertGanghjelpemiddel: Boolean?,
    val type: GanghjelpemiddelType?,
    val bruksområde: BruksområdeGanghjelpemiddel?,
    val detErLagetEnMålrettetPlan: Boolean?,
    val planenOppbevaresIKommunen: Boolean?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ElektriskVendesystemInfo(
    val sengForMontering: SengForVendesystemMontering?,
    val standardLakenByttesTilRiktigStørrelseAvNav: Boolean?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SengForVendesystemMontering(
    val hmsnr: String?,
    val navn: String?,
    val madrassbredde: Int?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SengeInfo(
    val påkrevdBehov: String?,
    val brukerOppfyllerPåkrevdBehov: Boolean?,
    val behovForSeng: String?,
    val behovForSengBegrunnelse: String?,
    val madrassValg: MadrassValg?,
    val høyGrindValg: HøyGrindValg?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HøyGrindValg(
    val erKjentMedTvangsAspekt: Boolean,
    val harForsøktOpptrening: Boolean,
    val harIkkeForsøktOpptreningBegrunnelse: String?,
    val erLagetPlanForOppfølging: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VarmehjelpemiddelInfo(
    val harHelseopplysningerFraFør: Boolean?,
    val legeBekrefterDiagnose: Boolean?,
    val opplysningerFraLegeOppbevaresIKommune: Boolean?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HjelpemiddelProdukt(
    val stockid: String?,
    val artid: String?,
    val prodid: String?,
    val artno: String?,
    val artname: String?,
    val adescshort: String?,
    val prodname: String?,
    val pshortdesc: String?,
    val artpostid: String?,
    val apostid: String?,
    val postrank: String?,
    val apostnr: String?,
    val aposttitle: String?,
    val newsid: String?,
    val isocode: String?,
    val isotitle: String?,
    var kategori: String?,
    @JsonAlias("technicalData", "techdata", "Techdata")
    var techdata: Array<Techdata>? = emptyArray(),
    var techdataAsText: String?,
    var paakrevdGodkjenningskurs: PaakrevdGodkjenningsKurs?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Techdata(
    val techlabeldk: String?,
    val datavalue: String?,
    val techdataunit: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaakrevdGodkjenningsKurs(
    val kursId: Int?,
    val tittel: String?,
    val isokode: String?,
    val formidlersGjennomforing: FormidlersGjennomforingAvKurs?,
)

enum class FormidlersGjennomforingAvKurs {
    GODKJENNINGSKURS_DB,
    VALGT_AV_FORMIDLER,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Kabin(
    val brukerOppfyllerKrav: Boolean,
    val kanIkkeAvhjelpesMedEnklereArsak: String?,
    val kanIkkeAvhjelpesMedEnklereBegrunnelse: String?,
    val arsakForBehovBegrunnelse: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AppInfo(
    val brukerHarProvdProvelisens: Boolean,
    val stottepersonSkalAdministrere: Boolean,
    val stottepersonHarProvdProvelisens: Boolean?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonlofterInfo(
    val harBehovForSeilEllerSele: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RullestolInfo(
    val skalBrukesIBil: Boolean?,
    val sitteputeValg: SitteputeValg?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Tilbehor(
    val hmsnr: String,
    val antall: Int?,
    val navn: String,
    val automatiskGenerert: AutomatiskGenerertTilbehor?,
    val brukAvForslagsmotoren: BrukAvForslagsmotoren?,
    val begrunnelse: String?,
    val fritakFraBegrunnelseÅrsak: FritakFraBegrunnelseÅrsak?,
)

enum class AutomatiskGenerertTilbehor {
    Sittepute,
}

enum class FritakFraBegrunnelseÅrsak {
    ER_PÅ_BESTILLINGSORDNING,
    IKKE_I_PILOT,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BrukAvForslagsmotoren(
    val lagtTilFraForslagsmotoren: Boolean,
    val oppslagAvNavn: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HjelpemiddelVilkar(
    val vilkartekst: String,
    val checked: Boolean,
    val kreverTilleggsinfo: Boolean?,
    val tilleggsinfo: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Hast(
    val hasteårsaker: Set<Hasteårsak>,
    val hastBegrunnelse: String?,
)

enum class Hasteårsak {
    UTVIKLING_AV_TRYKKSÅR,
    TERMINALPLEIE,

    @Deprecated("Erstattet av _V2")
    UTSKRIVING_FRA_SYKEHUS_SOM_IKKE_KAN_PLANLEGGES,

    @Deprecated("Erstattet av _V3")
    UTSKRIVING_FRA_SYKEHUS_SOM_IKKE_KAN_PLANLEGGES_V2,
    UTSKRIVING_FRA_SYKEHUS_SOM_IKKE_KAN_PLANLEGGES_V3,
    RASK_FORVERRING_AV_ALVORLIG_DIAGNOSE,
    ANNET,
}
