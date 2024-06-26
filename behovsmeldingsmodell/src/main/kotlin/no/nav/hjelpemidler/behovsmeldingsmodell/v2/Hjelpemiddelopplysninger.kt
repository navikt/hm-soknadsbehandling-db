package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Bruksarena
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Kroppsmål

// TODO bedre navn? Datum/InnsendersOpplysninger/Begrunnelser
data class Hjelpemiddelopplysninger(
    // Generell info som er aktuell for alle typer hjelpemidler. Dvs. ikke knyttet til kategori
    val generelt: GenerellInfo,
    val sittepute: SitteputeInfo? = null,
    val madrass: MadrassInfo? = null,
    val manuellRullestol: ManuellRullestolInfo? = null,
    val elektriskRullestolInfo: ElektriskRullestolInfo? = null,
    val personløfterInfo: PersonløfterInfo? = null,
    val appInfo: AppInfo? = null,
    val varmehjelpemiddelInfo: VarmehjelpemiddelInfo? = null,
    val sengInfo: SengInfo? = null,
    val elektriskVendesystemInfo: ElektriskVendesystemInfo? = null,
    val ganghjelpemiddelInfo: GanghjelpemiddelInfo? = null,
    val posisjoneringssystemInfo: PosisjoneringssystemInfo? = null,
    val posisjoneringsputeForBarnInfo: PosisjoneringsputeForBarnInfo? = null,
    val oppreisningsstolInfo: OppreisningsstolInfo? = null,
    val diverseInfo: DiverseInfo? = null,
)

data class GenerellInfo(
    val bruksarena: Listeverdi<Bruksarena>,
    val årsakForAntall: Enkeltverdi<ÅrsakForAntall>? = null,
    val andreKommentarer: Fritekst? = null, // tidligere "tilleggsinfo"
    val begrunnelseForLavereRangering: Fritekst? = null,       // tidligere "begrunnelsen" når rangering < 1
    val begrunnelseForKanIkkeHaTilsvarende: Fritekst? = null,  // tidligere "begrunnelsen" når rangering == 1
) {
    enum class ÅrsakForAntall {
        BEHOV_I_FLERE_ETASJER,
        BEHOV_I_FLERE_ROM,
        BEHOV_INNENDØRS_OG_UTENDØRS,
        BEHOV_FOR_FLERE_PUTER_FOR_RULLESTOL,
        BEHOV_FOR_JEVNLIG_VASK_ELLER_VEDLIKEHOLD,
        BRUKER_HAR_TO_HJEM,
        PUTENE_SKAL_KOMBINERES_I_POSISJONERING,
        BEHOV_HJEMME_OG_I_BARNEHAGE,
        PUTENE_SKAL_SETTES_SAMMEN_VED_BRUK,
        ANNET_BEHOV,
    }
}

data class SitteputeInfo (
    val behov: Listeverdi<SitteputeBehov>,
    val sitteputeSkalBrukesIRullestolFraNav: Enkeltverdi<Boolean>? = null,
) {
    enum class SitteputeBehov {
        FOREBYGGE_TRYKKSÅR,
        HINDRE_VIDERE_UTVIKLING_AV_TRYKKSÅR,
        ANDRE_SPESIELLE_BEHOV,
    }
}

data class MadrassInfo (
    val behov: Listeverdi<MadrassBehov>,
) {
    enum class MadrassBehov {
        FOREBYGGE_TRYKKSÅR,
        HINDRE_VIDERE_UTVIKLING_AV_TRYKKSÅR,
        TERMINALFASE,
    }
}

data class ManuellRullestolInfo(
    val skalBrukesIBil: Enkeltverdi<Boolean>,
    val sitteputeValg: Enkeltverdi<SitteputeValg>,
    val kroppsmål: KroppsmålForRullestol,
) {
    enum class SitteputeValg {
        TRENGER_SITTEPUTE,
        HAR_FRA_FØR,
    }
}

data class KroppsmålForRullestol (
    val setebredde: Int,
    val lårlengde: Int,
    val legglengde: Int,
    val høyde: Int,
    val kroppsvekt: Int,
): Visbar {
    override val visning = Visning.MULTI_INLINE
}

data class ElektriskRullestolInfo(
    // TODO er denne i bruk?
    //  val godkjenningskurs: Boolean?,

    val kroppsmål: KroppsmålForRullestol,

    val vurderinger: Listeverdi<Vurderinger>,
    val oppbevaringOgLagring: Enkeltverdi<Boolean>, // oppbevaringInfo som evt. fritekst her
    val kjentMedForsikring: Enkeltverdi<Boolean>,
    val harSpesialsykkel: Enkeltverdi<Boolean>,
    val plasseringAvHendel: Enkeltverdi<Hendelplassering>,

    // Kabin
    val brukerOppfyllerKrav: Enkeltverdi<Boolean>,
    val kanIkkeAvhjelpesMedEnklereÅrsak: Enkeltverdi<Kabinbehov>, // brukerOppfyllerKrav == true
    val årsakForBehovBegrunnelse: Fritekst?, // brukerOppfyllerKrav == false
) {
    enum class Vurderinger {
        KAN_BETJENE_MANUELL_STYRING,
        KAN_BETJENE_MOTORISERT_STYRING,
        KAN_FERDES_SIKKERT_I_TRAFIKKEN,
        VESENTLIG_NEDSATT_GANGFUNKSJON_IKKE_TRANSPORT,
    }

    enum class Hendelplassering {
        VENSTRE, HØYRE,
    }

    enum class Kabinbehov {
        HAR_LUFTVEISPROBLEMER,
        BEGRENSNING_VED_FUNKSJONSNEDSETTELSE,
        ANNET,
    }
}

data class PersonløfterInfo(
    val harBehovForSeilEllerSele: Enkeltverdi<Boolean>,
)

data class AppInfo(
    val brukerHarPrøvdPrøvelisens: Enkeltverdi<Boolean>,
    val støttepersonSkalAdministrere: Enkeltverdi<Boolean>,
    val støttepersonHarPrøvdPrøvelisens: Enkeltverdi<Boolean>?,
)

data class VarmehjelpemiddelInfo(
    val harHelseopplysningerFraFør: Enkeltverdi<OpplysningerFraLege>?,
    val harHelseopplysningerIKommunen: Listeverdi<OpplysningerFraLege>?,
) {
    enum class OpplysningerFraLege {
        AUTOMATISK_SJEKKET,
        LEGEN_BEKREFTER_DIAGNOSEN,
        OPPLYSNINGENE_OPPBEVARES_I_KOMMUNEN,
    }
}

data class SengInfo(
    val madrassValg: Enkeltverdi<MadrassValg>?,

    val harDysfunksjoneltSøvnmønster: Enkeltverdi<Boolean>?,
    val harSterkeUfrivilligeBevegelser: Enkeltverdi<Boolean>?,
    val påkrevdBehov: String?,
    val brukerOppfyllerPåkrevdBehov: Boolean?,
    val behovForSeng: String?,
    val behovForSengBegrunnelse: String?,

    val høyGrindValg: HøyGrindValg?,
) {
    enum class MadrassValg {
        TRENGER_MADRASS,
        HAR_FRA_FØR,
    }
}

data class HøyGrindValg(
    @JsonProperty("erKjentMedTvangsAspekt")
    val erKjentMedTvangsaspekt: Boolean,
    val harForsøktOpptrening: Boolean,
    val harIkkeForsøktOpptreningBegrunnelse: String?,
    val erLagetPlanForOppfølging: Boolean,
)

data class OppreisningsstolInfo(
    val kanBrukerReiseSegSelvFraVanligStol: Bolskverdi,
    val behov: Set<OppreisningsstolBehov>?,
    val behovForStolBegrunnelse: String?,
    @JsonProperty("sideBetjeningsPanel")
    val sidebetjeningspanel: SidebetjeningspanelPosisjon?,
    val bruksområde: OppreisningsstolBruksområde?,
    val annetTrekkKanBenyttes: Boolean,
    val løftType: OppreisningsstolLøftType,
)

data class DiverseInfo(
    @JsonProperty("takhoydeStottestangCm")
    val takhøydeStøttestangCm: Int? = null,

)

data class PosisjoneringsputeForBarnInfo(
    val bruksområde: PosisjoneringsputeForBarnBruk?,
    val brukerErOver26År: Boolean?,
    val detErLagetEnMålrettetPlan: Boolean?,
    val planenOppbevaresIKommunen: Boolean?,
)

data class PosisjoneringssystemInfo(
    val skalIkkeBrukesSomBehandlingshjelpemiddel: Boolean?,
    val skalIkkeBrukesTilRenSmertelindring: Boolean?,
    val behov: PosisjoneringsputeBehov?,
    val oppgaverIDagliglivet: Set<PosisjoneringsputeOppgaverIDagligliv>?,
    val oppgaverIDagliglivetAnnet: String?,
)

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

data class HjelpemiddelVilkår(
    @JsonProperty("vilkartekst")
    val vilkårstekst: String,
    @JsonProperty("checked")
    val avhuket: Boolean,
    val kreverTilleggsinfo: Boolean?,
    val tilleggsinfo: String?,
)