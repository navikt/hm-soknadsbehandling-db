package no.nav.hjelpemidler.behovsmeldingsmodell.v1

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.hjelpemidler.behovsmeldingsmodell.AutomatiskGenerertTilbehør
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovForSeng
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.Boform
import no.nav.hjelpemidler.behovsmeldingsmodell.Brukerkilde
import no.nav.hjelpemidler.behovsmeldingsmodell.BrukersituasjonVilkår
import no.nav.hjelpemidler.behovsmeldingsmodell.Bruksarena
import no.nav.hjelpemidler.behovsmeldingsmodell.BruksområdeGanghjelpemiddel
import no.nav.hjelpemidler.behovsmeldingsmodell.BytteÅrsak
import no.nav.hjelpemidler.behovsmeldingsmodell.FritakFraBegrunnelseÅrsak
import no.nav.hjelpemidler.behovsmeldingsmodell.GanghjelpemiddelType
import no.nav.hjelpemidler.behovsmeldingsmodell.Hasteårsak
import no.nav.hjelpemidler.behovsmeldingsmodell.HjelpemiddelProdukt
import no.nav.hjelpemidler.behovsmeldingsmodell.InnsenderRolle
import no.nav.hjelpemidler.behovsmeldingsmodell.KanIkkeAvhjelpesMedEnklereÅrsak
import no.nav.hjelpemidler.behovsmeldingsmodell.Kontaktperson
import no.nav.hjelpemidler.behovsmeldingsmodell.LeveringTilleggsinfo
import no.nav.hjelpemidler.behovsmeldingsmodell.MadrassValg
import no.nav.hjelpemidler.behovsmeldingsmodell.Oppfølgingsansvarlig
import no.nav.hjelpemidler.behovsmeldingsmodell.OppreisningsstolBehov
import no.nav.hjelpemidler.behovsmeldingsmodell.OppreisningsstolBruksområde
import no.nav.hjelpemidler.behovsmeldingsmodell.OppreisningsstolLøftType
import no.nav.hjelpemidler.behovsmeldingsmodell.Organisasjon
import no.nav.hjelpemidler.behovsmeldingsmodell.PlasseringType
import no.nav.hjelpemidler.behovsmeldingsmodell.PosisjoneringsputeBehov
import no.nav.hjelpemidler.behovsmeldingsmodell.PosisjoneringsputeForBarnBruk
import no.nav.hjelpemidler.behovsmeldingsmodell.PosisjoneringsputeOppgaverIDagligliv
import no.nav.hjelpemidler.behovsmeldingsmodell.SidebetjeningspanelPosisjon
import no.nav.hjelpemidler.behovsmeldingsmodell.Signaturtype
import no.nav.hjelpemidler.behovsmeldingsmodell.SitteputeValg
import no.nav.hjelpemidler.behovsmeldingsmodell.Utleveringsmåte
import no.nav.hjelpemidler.behovsmeldingsmodell.UtlevertType
import no.nav.hjelpemidler.behovsmeldingsmodell.ÅrsakForAntall
import no.nav.hjelpemidler.domain.geografi.Veiadresse
import no.nav.hjelpemidler.domain.geografi.lagVeiadresse
import no.nav.hjelpemidler.domain.person.Fødselsnummer
import no.nav.hjelpemidler.domain.person.HarPersonnavn
import no.nav.hjelpemidler.domain.person.Personnavn
import no.nav.hjelpemidler.domain.person.TilknyttetPerson
import no.nav.hjelpemidler.domain.person.lagPersonnavn
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
    override val fnr: Fødselsnummer,
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
    val borIPilotkommuneForHast: Boolean? = false,
) : TilknyttetPerson {
    val navn: Personnavn @JsonIgnore get() = lagPersonnavn(fornavn, etternavn = etternavn)
    val veiadresse: Veiadresse? @JsonIgnore get() = lagVeiadresse(adresse, postnummer, poststed)
    val kildeErPdl: Boolean @JsonIgnore get() = kilde == Brukerkilde.PDL
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
    @Deprecated("Erstattet med bruksarena på hvert enkelt hjm")
    @JsonProperty("bostedRadioButton")
    val boform: Boform?,

    @Deprecated("Erstattet med bruksarena på hvert enkelt hjm")
    val bruksarenaErDagliglivet: Boolean?,

    @Deprecated("Erstattet med bekreftedeVilkår")
    @JsonProperty("storreBehov")
    val størreBehov: Boolean?,
    @Deprecated("Erstattet med bekreftedeVilkår")
    val nedsattFunksjon: Boolean?,
    @Deprecated("Erstattet med bekreftedeVilkår")
    val praktiskeProblem: Boolean?,
    @Deprecated("Erstattet med bekreftedeVilkår")
    @JsonProperty("skalIkkeBrukesTilAndreFormaal")
    val skalIkkeBrukesTilAndreFormål: Boolean?,

    val funksjonsbeskrivelse: Funksjonsbeskrivelse?,
)

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
    val hjelpemiddelformidlerFornavn: String,
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
    val hjelpemiddelformidlerKommunenavn: String?,

    // Oppfølgingsansvarlig

    @JsonProperty("opfRadioButton")
    val oppfølgingsansvarlig: Oppfølgingsansvarlig?,

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
            kommunenavn = hjelpemiddelformidlerKommunenavn,
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

    val harFritekstUnderOppfølgingsansvarlig: Boolean
        @JsonIgnore
        get() = !oppfølgingsansvarligAnsvarFor.isNullOrBlank()

    val harFritekstUnderLevering: Boolean
        @JsonIgnore
        get() = utleveringMerknad.isNotBlank()

    data class Hjelpemiddelformidler(
        override val navn: Personnavn,
        val arbeidssted: String,
        val stilling: String,
        val telefon: String,
        val adresse: Veiadresse,
        val epost: String,
        val treffesEnklest: String,
        val kommunenavn: String?,
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
    val beskrivelse: String, // = navn
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
    val kanIkkeTilsvarende: Boolean? = null,
    val navn: String? = null, // = beskrivelse
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
) {
    val årsakForAntallEnum: ÅrsakForAntall? = when (årsakForAntall) {
        "Behov i flere etasjer" -> ÅrsakForAntall.BEHOV_I_FLERE_ETASJER
        "Behov i flere rom" -> ÅrsakForAntall.BEHOV_I_FLERE_ROM
        "Behov både innendørs og utendørs" -> ÅrsakForAntall.BEHOV_INNENDØRS_OG_UTENDØRS
        "Behov for pute til flere rullestoler eller sitteenheter" -> ÅrsakForAntall.BEHOV_FOR_FLERE_PUTER_FOR_RULLESTOL
        "Behov for jevnlig vask eller vedlikehold" -> ÅrsakForAntall.BEHOV_FOR_JEVNLIG_VASK_ELLER_VEDLIKEHOLD
        "Bruker har to hjem" -> ÅrsakForAntall.BRUKER_HAR_TO_HJEM
        "Annet behov" -> ÅrsakForAntall.ANNET_BEHOV
        "PUTENE_SKAL_KOMBINERES_I_POSISJONERING" -> ÅrsakForAntall.PUTENE_SKAL_KOMBINERES_I_POSISJONERING
        "BEHOV_HJEMME_OG_I_BARNEHAGE" -> ÅrsakForAntall.BEHOV_HJEMME_OG_I_BARNEHAGE
        "PUTENE_SKAL_SETTES_SAMMEN_VED_BRUK" -> ÅrsakForAntall.PUTENE_SKAL_SETTES_SAMMEN_VED_BRUK
        else -> null
    }
}

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
    val påkrevdBehov: BehovForSeng?,
    val brukerOppfyllerPåkrevdBehov: Boolean?,
    val behovForSeng: BehovForSeng?,
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
    /**
     * Her har det sneket seg inn en skrivefeil både i koden og i enkelte tekster.
     * Det er oppbevaring og LADING vi er ute etter.
     */
    @JsonProperty("oppbevaringOgLagring")
    val oppbevaringOgLading: Boolean?,
    val oppbevaringInfo: String?,
    val kjentMedForsikring: Boolean?,
    val harSpesialsykkel: Boolean?,
    val plasseringAvHendel: PlasseringType?,
    val kabin: Kabin?,
)

data class Kabin(
    val brukerOppfyllerKrav: Boolean,
    @JsonProperty("kanIkkeAvhjelpesMedEnklereArsak")
    val kanIkkeAvhjelpesMedEnklereÅrsak: KanIkkeAvhjelpesMedEnklereÅrsak?,
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
    val begrunnelse: String?,
    val fritakFraBegrunnelseÅrsak: FritakFraBegrunnelseÅrsak?,
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
