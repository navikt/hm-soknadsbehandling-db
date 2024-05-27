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
    val hast: Hast? = null,
)

data class Innsender(
    val godkjenningskurs: List<Godkjenningskurs> = emptyList(),
    val organisasjoner: List<Organisasjon> = emptyList(),
    val somRolle: InnsenderRolle,
    val tjenestligeBehovForUtlånsoversikt: Set<String> = emptySet(),
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
    val telefon: String,
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
    val navn: Personnavn @JsonIgnore get() = lagPersonnavn(fornavn, etternavn)
    val veiadresse: Veiadresse? @JsonIgnore get() = lagVeiadresse(adresse, postnummer, poststed)
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
    val bekreftedeVilkår: Set<BrukersituasjonVilkår> = emptySet(),
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
    val hjelpemidler: List<Hjelpemiddel>,
    @JsonProperty("hjelpemiddelTotaltAntall")
    val totaltAntall: Int,
) {
    val påkrevdeGodkjenningskurs: List<HjelpemiddelProdukt.PåkrevdGodkjenningskurs>
        @JsonIgnore get() = hjelpemidler
            .mapNotNull { it.produkt?.påkrevdGodkjenningskurs }
            .distinctBy { it.kursId }
            .sortedBy { it.tittel }
}

data class Levering(
    // Hjelpemiddelformidler

    @JsonProperty("hmfFornavn")
    private val hjelpemiddelformidlerFornavn: String,
    @JsonProperty("hmfEtternavn")
    val hjelpemiddelformidlerEtternavn: String,
    @JsonProperty("hmfArbeidssted")
    val hjelpemiddelformidlerArbeidssted: String,
    @JsonProperty("hmfStilling")
    val hjelpemiddelformidlerStilling: String,
    @JsonProperty("hmfTelefon")
    val hjelpemiddelformidlerTelefon: String,
    @JsonProperty("hmfEpost")
    val hjelpemiddelformidlerEpost: String,
    @JsonProperty("hmfPostadresse")
    val hjelpemiddelformidlerPostadresse: String,
    @JsonProperty("hmfPostnr")
    val hjelpemiddelformidlerPostnummer: String,
    @JsonProperty("hmfPoststed")
    val hjelpemiddelformidlerPoststed: String,
    @JsonProperty("hmfTreffesEnklest")
    val hjelpemiddelformidlerTreffesEnklest: String,

    // Oppfølgingsansvarlig

    @JsonProperty("opfRadioButton")
    val oppfølgingsansvarlig: Oppfølgingsansvarlig,

    // Oppfølgingsansvarlig -> Annen person er ansvarlig

    @JsonProperty("opfFornavn")
    val oppfølgingsansvarligFornavn: String?,
    @JsonProperty("opfEtternavn")
    val oppfølgingsansvarligEtternavn: String?,
    @JsonProperty("opfArbeidssted")
    val oppfølgingsansvarligArbeidssted: String?,
    @JsonProperty("opfStilling")
    val oppfølgingsansvarligStilling: String?,
    @JsonProperty("opfTelefon")
    val oppfølgingsansvarligTelefon: String?,
    @JsonProperty("opfAnsvarFor")
    val oppfølgingsansvarligAnsvarFor: String?,

    // Utleveringsmåte

    @JsonProperty("utleveringsmaateRadioButton")
    val utleveringsmåte: Utleveringsmåte?,

    // Utleveringsmåte -> Kontaktperson

    @JsonProperty("utleveringskontaktpersonRadioButton")
    val utleveringKontaktperson: Kontaktperson?,

    // Utleveringsmåte -> Annen kontaktperson

    val utleveringFornavn: String?,
    val utleveringEtternavn: String?,
    val utleveringTelefon: String?,

    // Utleveringsmåte -> Annen utleveringsadresse

    val utleveringPostadresse: String?,
    @JsonProperty("utleveringPostnr")
    val utleveringPostnummer: String?,
    val utleveringPoststed: String?,

    // Kommentarer til utlevering

    @JsonProperty("merknadTilUtlevering")
    val utleveringMerknad: String,

    // Tilleggsinformasjon

    val tilleggsinfo: Set<LeveringTilleggsinfo> = emptySet(),
) {
    val hjelpemiddelformidler: Hjelpemiddelformidler
        @JsonIgnore
        get() = Hjelpemiddelformidler(
            navn = lagPersonnavn(
                fornavn = hjelpemiddelformidlerFornavn,
                etternavn = hjelpemiddelformidlerEtternavn,
            ),
            arbeidssted = hjelpemiddelformidlerArbeidssted,
            stilling = hjelpemiddelformidlerStilling,
            telefon = hjelpemiddelformidlerTelefon,
            adresse = lagVeiadresse(
                adresse = hjelpemiddelformidlerPostadresse,
                postnummer = hjelpemiddelformidlerPostnummer,
                poststed = hjelpemiddelformidlerPoststed,
            ),
            epost = hjelpemiddelformidlerEpost,
            treffesEnklest = hjelpemiddelformidlerTreffesEnklest,
        )

    val annenOppfølgingsansvarlig: AnnenOppfølgingsansvarlig?
        @JsonIgnore
        get() = if (oppfølgingsansvarlig != Oppfølgingsansvarlig.ANNEN_OPPFØLGINGSANSVARLIG) {
            null
        } else {
            AnnenOppfølgingsansvarlig(
                navn = lagPersonnavn(
                    fornavn = checkNotNull(oppfølgingsansvarligFornavn),
                    etternavn = checkNotNull(oppfølgingsansvarligEtternavn),
                ),
                arbeidssted = checkNotNull(oppfølgingsansvarligArbeidssted),
                stilling = checkNotNull(oppfølgingsansvarligStilling),
                telefon = checkNotNull(oppfølgingsansvarligTelefon),
                ansvarFor = checkNotNull(oppfølgingsansvarligAnsvarFor),
            )
        }

    val annenKontaktperson: AnnenKontaktperson?
        @JsonIgnore
        get() = if (utleveringKontaktperson != Kontaktperson.ANNEN_KONTAKTPERSON) {
            null
        } else {
            AnnenKontaktperson(
                navn = lagPersonnavn(
                    fornavn = checkNotNull(utleveringFornavn),
                    etternavn = checkNotNull(utleveringEtternavn),
                ),
                telefon = checkNotNull(utleveringTelefon),
            )
        }

    val annenUtleveringsadresse: Veiadresse?
        @JsonIgnore
        get() = if (utleveringsmåte != Utleveringsmåte.ANNEN_BRUKSADRESSE) {
            null
        } else {
            lagVeiadresse(
                adresse = checkNotNull(utleveringPostadresse),
                postnummer = checkNotNull(utleveringPostnummer),
                poststed = checkNotNull(utleveringPoststed),
            )
        }

    data class Hjelpemiddelformidler(
        override val navn: Personnavn,
        val arbeidssted: String,
        val stilling: String,
        val telefon: String,
        val adresse: Veiadresse,
        val epost: String,
        val treffesEnklest: String,
    ) : HarPersonnavn

    data class AnnenOppfølgingsansvarlig(
        override val navn: Personnavn,
        val arbeidssted: String,
        val stilling: String,
        val telefon: String,
        val ansvarFor: String,
    ) : HarPersonnavn

    data class AnnenKontaktperson(
        override val navn: Personnavn,
        val telefon: String,
    ) : HarPersonnavn
}

data class Hast(
    val hasteårsaker: Set<Hasteårsak>,
    val hastBegrunnelse: String?,
)

data class Hjelpemiddel(
    val antall: Int,
    @JsonProperty("arsakForAntall")
    val årsakForAntall: String? = null,
    @JsonProperty("arsakForAntallBegrunnelse")
    val årsakForAntallBegrunnelse: String? = null,
    val beskrivelse: String,
    val hjelpemiddelkategori: String,
    @JsonProperty("hmsNr")
    val hmsnr: String,
    @JsonProperty("tilleggsinformasjon")
    val tilleggsinfo: String,
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
    @JsonProperty("sengeInfo")
    val sengInfo: SengInfo? = null,
    val elektriskVendesystemInfo: ElektriskVendesystemInfo? = null,
    val ganghjelpemiddelInfo: GanghjelpemiddelInfo? = null,
    val posisjoneringssystemInfo: PosisjoneringssystemInfo? = null,
    val posisjoneringsputeForBarnInfo: PosisjoneringsputeForBarnInfo? = null,
    @JsonProperty("oppreisningsStolInfo")
    val oppreisningsstolInfo: OppreisningsstolInfo? = null,
    val diverseInfo: DiverseInfo? = null,
    val bytter: List<Bytte> = emptyList(),
    val bruksarena: Set<Bruksarena> = emptySet(),
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

data class SengInfo(
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
