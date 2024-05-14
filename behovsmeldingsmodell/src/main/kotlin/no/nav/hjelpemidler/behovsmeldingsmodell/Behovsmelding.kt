package no.nav.hjelpemidler.behovsmeldingsmodell

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.util.UUID

data class Behovsmelding(
    val behovsmeldingType: BehovsmeldingType = BehovsmeldingType.SØKNAD,
    val bestillingsordningsjekk: Bestillingsordningsjekk? = null,
    @JsonProperty("soknad")
    val søknad: Søknad? = null,
    val brukerpassbytte: Brukerpassbytte? = null,
    val id: UUID? = søknad?.id,
)

data class Søknad(
    val id: UUID,
    val bruker: Bruker,
    val brukersituasjon: Brukersituasjon,
    @JsonProperty("date")
    val dato: LocalDate?,
    val hjelpemidler: Hjelpemidler,
    val levering: Levering,
    val innsender: Innsender?,
    val erHast: Boolean = false, // = false for bakoverkompatibilitet, kan fjernes etter lansering
)

data class Innsender(
    val godkjenningskurs: List<Godkjenningskurs> = emptyList(),
    val organisasjoner: List<Organisasjon> = emptyList(),
    val somRolle: InnsenderRolle,
)

data class Godkjenningskurs(
    val id: Int,
    val title: String,
    val kilde: String,
)

data class Bruker(
    @JsonProperty("fnummer")
    val fnr: Fødselsnummer,
    val fornavn: String,
    val etternavn: String,
    @JsonProperty("signatur")
    val signaturtype: Signaturtype?,
    @JsonProperty("telefonNummer")
    val telefonnummer: String,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val kommunenummer: String?,
    val brukernummer: String?,
    val kilde: Brukerkilde?,
    @JsonProperty("kroppsmaal")
    val kroppsmål: Kroppsmål?,
    val erInformertOmRettigheter: Boolean?,
) {
    val navn: Personnavn @JsonIgnore get() = Personnavn(fornavn, etternavn)
}

data class Kroppsmål(
    val setebredde: Int?,
    @JsonProperty("laarlengde")
    val lårlengde: Int?,
    val legglengde: Int?,
    @JsonProperty("hoyde")
    val høyde: Int?,
    val kroppsvekt: Int?,
)

data class Brukersituasjon(
    val bekreftedeVilkår: List<BrukersituasjonVilkår> = emptyList(),
    @JsonProperty("nedsattFunksjonTypes")
    val funksjonsnedsettelser: Funksjonsnedsettelser,
)

data class Funksjonsnedsettelser(
    val bevegelse: Boolean,
    val kognisjon: Boolean,
    @JsonProperty("horsel")
    val hørsel: Boolean,
)

data class Hjelpemidler(
    @JsonProperty("hjelpemiddelListe")
    val hjelpemidler: List<HjelpemiddelItem>,
    @JsonProperty("hjelpemiddelTotaltAntall")
    val totaltAntall: Int,
)

data class Levering(
    val hmfArbeidssted: String,
    val hmfEpost: String,
    val hmfFornavn: String,
    val hmfEtternavn: String,
    val hmfPostadresse: String,
    val hmfPostnr: String,
    val hmfPoststed: String,
    val hmfStilling: String,
    val hmfTelefon: String,
    val hmfTreffesEnklest: String,
    val merknadTilUtlevering: String,
    val opfAnsvarFor: String?,
    val opfArbeidssted: String?,
    val opfFornavn: String?,
    val opfEtternavn: String?,
    @JsonProperty("opfRadioButton")
    val opf: Oppfølger,
    val opfStilling: String?,
    val opfTelefon: String?,
    val utleveringFornavn: String?,
    val utleveringEtternavn: String?,
    val utleveringPostadresse: String?,
    val utleveringPostnr: String?,
    val utleveringPoststed: String?,
    val utleveringTelefon: String?,
    @JsonProperty("utleveringskontaktpersonRadioButton")
    val utleveringKontaktperson: Kontaktperson?,
    @JsonProperty("utleveringsmaateRadioButton")
    val utleveringsmåte: Utleveringsmåte?,
    val tilleggsinfo: List<LeveringTilleggsinfo> = emptyList(),
)

data class HjelpemiddelItem(
    val antall: Int,
    @JsonProperty("arsakForAntall")
    val årsakForAntall: String? = null,
    @JsonProperty("arsakForAntallBegrunnelse")
    val årsakForAntallBegrunnelse: String? = null,
    val beskrivelse: String,
    val hjelpemiddelkategori: String,
    @JsonProperty("hmsNr")
    val hmsnr: String,
    val tilleggsinformasjon: String,
    val uniqueKey: String,
    val utlevertFraHjelpemiddelsentralen: Boolean,
    @JsonProperty("vilkaroverskrift")
    val vilkårsoverskrift: String? = null,
    @JsonProperty("vilkarliste")
    val vilkår: List<HjelpemiddelVilkår>? = null,
    @JsonProperty("tilbehorListe")
    val tilbehør: List<Tilbehør>? = null,
    @JsonProperty("begrunnelsen") // hvorfor bestemt form?
    val begrunnelse: String? = null,
    @JsonProperty("kanIkkeTilsvarande") // nynorsk
    val kanIkkeTilsvarende: String? = null,
    val navn: String? = null,
    val produkt: HjelpemiddelProdukt? = null,
    val rullestolInfo: RullestolInfo? = null,
    val utlevertInfo: UtlevertInfo? = null,
    @JsonProperty("personlofterInfo")
    val personløfterInfo: PersonløfterInfo? = null,
    val elektriskRullestolInfo: ElektriskRullestolInfo? = null,
    val appInfo: AppInfo? = null,
    val varmehjelpemiddelInfo: VarmehjelpemiddelInfo? = null,
    val sengeInfo: SengeInfo? = null,
    val elektriskVendesystemInfo: ElektriskVendesystemInfo? = null,
    val ganghjelpemiddelInfo: GanghjelpemiddelInfo? = null,
    val posisjoneringssystemInfo: PosisjoneringssystemInfo? = null,
    val posisjoneringsputeForBarnInfo: PosisjoneringsputeForBarnInfo? = null,
    @JsonProperty("oppreisningsStolInfo")
    val oppreisningsstolInfo: OppreisningsstolInfo? = null,
    val diverseInfo: DiverseInfo? = null,
    val bytter: List<Bytte> = emptyList(),
    val bruksarena: List<Bruksarena> = emptyList(),
    val hasteårsaker: List<Hasteårsak> = emptyList(),
)

data class Bytte(
    val erTilsvarende: Boolean,
    val hmsnr: String,
    val serienr: String? = null,
    val hjmNavn: String,
    val hjmKategori: String,
    val årsak: BytteÅrsak? = null,
)

data class OppreisningsstolInfo(
    val kanBrukerReiseSegSelvFraVanligStol: Boolean,
    val behov: List<OppreisningsstolBehov>?,
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
    val sitteputeSkalBrukesIRullestolFraNav: Boolean? = null,
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
    val oppgaverIDagliglivet: List<PosisjoneringsputeOppgaverIDagligliv>?,
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

data class SengeInfo(
    val påkrevdBehov: String?,
    val brukerOppfyllerPåkrevdBehov: Boolean?,
    val behovForSeng: String?,
    val behovForSengBegrunnelse: String?,
    val madrassValg: MadrassValg?,
    val høyGrindValg: HøyGrindValg?,
)

data class AppInfo(
    @JsonProperty("brukerHarProvdProvelisens")
    val brukerHarPrøvdPrøvelisens: Boolean,
    @JsonProperty("stottepersonSkalAdministrere")
    val støttepersonSkalAdministrere: Boolean,
    @JsonProperty("stottepersonHarProvdProvelisens")
    val støttepersonHarPrøvdPrøvelisens: Boolean?,
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
    @JsonProperty("kanIkkeAvhjelpesMedEnklereArsak")
    val kanIkkeAvhjelpesMedEnklereÅrsak: String?,
    val kanIkkeAvhjelpesMedEnklereBegrunnelse: String?,
    @JsonProperty("arsakForBehovBegrunnelse")
    val årsakForBehovBegrunnelse: String?,
)

data class PersonløfterInfo(
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

data class HøyGrindValg(
    @JsonProperty("erKjentMedTvangsAspekt")
    val erKjentMedTvangsaspekt: Boolean,
    val harForsøktOpptrening: Boolean,
    val harIkkeForsøktOpptreningBegrunnelse: String?,
    val erLagetPlanForOppfølging: Boolean,
)

data class Tilbehør(
    val hmsnr: String,
    val antall: Int?,
    val navn: String,
    val automatiskGenerert: AutomatiskGenerertTilbehør?,
    val brukAvForslagsmotoren: BrukAvForslagsmotoren?,
)

data class BrukAvForslagsmotoren(
    val lagtTilFraForslagsmotoren: Boolean,
    val oppslagAvNavn: Boolean,
)

data class HjelpemiddelVilkår(
    @JsonProperty("vilkartekst")
    val vilkårstekst: String,
    @JsonProperty("checked")
    val avhuket: Boolean,
    val kreverTilleggsinfo: Boolean?,
    val tilleggsinfo: String?,
)
