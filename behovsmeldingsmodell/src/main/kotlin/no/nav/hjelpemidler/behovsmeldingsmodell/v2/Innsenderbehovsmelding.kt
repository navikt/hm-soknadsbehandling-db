package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.Brukerkilde
import no.nav.hjelpemidler.behovsmeldingsmodell.BrukersituasjonVilkårtype
import no.nav.hjelpemidler.behovsmeldingsmodell.BruksarenaV2
import no.nav.hjelpemidler.behovsmeldingsmodell.FritakFraBegrunnelseÅrsak
import no.nav.hjelpemidler.behovsmeldingsmodell.Funksjonsnedsettelser
import no.nav.hjelpemidler.behovsmeldingsmodell.InnsenderRolle
import no.nav.hjelpemidler.behovsmeldingsmodell.KontaktpersonV2
import no.nav.hjelpemidler.behovsmeldingsmodell.LeveringTilleggsinfo
import no.nav.hjelpemidler.behovsmeldingsmodell.OppfølgingsansvarligV2
import no.nav.hjelpemidler.behovsmeldingsmodell.Prioritet
import no.nav.hjelpemidler.behovsmeldingsmodell.Signaturtype
import no.nav.hjelpemidler.behovsmeldingsmodell.UtleveringsmåteV2
import no.nav.hjelpemidler.behovsmeldingsmodell.UtlevertTypeV2
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Bestillingsordningsjekk
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Bytte
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Funksjonsbeskrivelse
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Godkjenningskurs
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Hast
import no.nav.hjelpemidler.domain.geografi.Veiadresse
import no.nav.hjelpemidler.domain.person.Fødselsnummer
import no.nav.hjelpemidler.domain.person.Personnavn
import no.nav.hjelpemidler.domain.person.TilknyttetPerson
import org.owasp.html.HtmlPolicyBuilder
import java.time.LocalDate
import java.util.UUID

data class Innsenderbehovsmelding(
    val bruker: Bruker,
    val brukersituasjon: Brukersituasjon,
    val hjelpemidler: Hjelpemidler,
    val levering: Levering,
    val innsender: Innsender,

    val metadata: InnsenderbehovsmeldingMetadata,

    override val id: UUID,
    override val type: BehovsmeldingType,
    override val innsendingsdato: LocalDate,
    override val skjemaversjon: Int = 2,
    override val hjmBrukersFnr: Fødselsnummer = bruker.fnr,
    override val prioritet: Prioritet = tilPrioritet(levering.hast),
) : BehovsmeldingBase

data class InnsenderbehovsmeldingMetadata(
    val bestillingsordningsjekk: Bestillingsordningsjekk?,
)

data class Bruker(
    override val fnr: Fødselsnummer,
    val navn: Personnavn,
    val signaturtype: Signaturtype,
    val telefon: String,
    val veiadresse: Veiadresse?,
    val kommunenummer: String?,
    val brukernummer: String?,
    val kilde: Brukerkilde?,
    val legacyopplysninger: List<EnkelOpplysning>, // for visning av opplysninger som bare finnes i eldre behovsmeldinger
) : TilknyttetPerson {
    val kildeErPdl: Boolean @JsonIgnore get() = kilde == Brukerkilde.PDL
}

data class Brukersituasjon(
    val bekreftedeVilkår: Set<BrukersituasjonVilkårV2>,
    val funksjonsnedsettelser: Set<Funksjonsnedsettelser>,
    val funksjonsbeskrivelse: Funksjonsbeskrivelse?,
)

data class BrukersituasjonVilkårV2(
    val vilkårtype: BrukersituasjonVilkårtype,
    val tekst: LokalisertTekst,
)

data class Levering(
    val hjelpemiddelformidler: no.nav.hjelpemidler.behovsmeldingsmodell.v1.Levering.Hjelpemiddelformidler,

    val oppfølgingsansvarlig: OppfølgingsansvarligV2,
    val annenOppfølgingsansvarlig: no.nav.hjelpemidler.behovsmeldingsmodell.v1.Levering.AnnenOppfølgingsansvarlig?,

    /**
     * utleveringsmåte == null -> formidler har ikke fått spm om utlevering fordi det ikke er behov for denne infoen.
     * Skjer når hvert hjm. er markert som utlevert eller ikke trenger info om utlevering (feks for apper hvor lisens
     * sendes til MinSide på nav.no, eller til folkereg. adresse for barn under 18 år).
     */
    val utleveringsmåte: UtleveringsmåteV2?,
    val annenUtleveringsadresse: Veiadresse?,

    // utleveringKontaktperson == null => alle hjm. er allerede utlevert
    val utleveringKontaktperson: KontaktpersonV2?,
    val annenKontaktperson: no.nav.hjelpemidler.behovsmeldingsmodell.v1.Levering.AnnenKontaktperson?,

    val utleveringMerknad: String,

    val hast: Hast?,

    /**
     * Inneholder ekstra informasjon som automatisk er utledet. Dvs. det er ikke noe formidler har svart på (direkte).
     */
    val automatiskUtledetTilleggsinfo: Set<LeveringTilleggsinfo> = emptySet(),
) {
    val harFritekstUnderOppfølgingsansvarlig: Boolean
        @JsonIgnore
        get() = !annenOppfølgingsansvarlig?.ansvarFor.isNullOrBlank()

    val harFritekstUnderLevering: Boolean
        @JsonIgnore
        get() = utleveringMerknad.isNotBlank()
}

data class Innsender(
    val rolle: InnsenderRolle,
    val kurs: List<Godkjenningskurs>,
    val sjekketUtlånsoversiktForKategorier: Set<Iso6>,
)

data class Hjelpemidler(
    val hjelpemidler: List<Hjelpemiddel>,
    val totaltAntall: Int,
)

data class Hjelpemiddel(
    val antall: Int,
    val produkt: HjelpemiddelProdukt,
    val tilbehør: List<Tilbehør>,
    val bytter: List<Bytte>,
    val bruksarenaer: Set<BruksarenaV2>,
    val utlevertinfo: Utlevertinfo,
    val opplysninger: List<Opplysning>,
    val varsler: List<Varsel>,
)

data class HjelpemiddelProdukt(
    val hmsArtNr: String,
    val artikkelnavn: String,
    val iso8: Iso8,
    val iso8Tittel: String,
    val delkontrakttittel: String,
    val sortimentkategori: String, // fra digithot-sortiment

    /*
    null -> ikke på rammeavtale
    Har i sjeldne tilfeller skjedd at formidler får søkt om produkt som ikke lenger er på rammeavtale, antageligvis pga
    endring i produkter på rammeavtale etter lansering av rammeavtalen.
     */
    val rangering: Int?,
)

data class Tilbehør(
    val hmsArtNr: String,
    val navn: String,
    val antall: Int,
    val begrunnelse: String?,
    val fritakFraBegrunnelseÅrsak: FritakFraBegrunnelseÅrsak?,
)

data class Utlevertinfo(
    val alleredeUtlevertFraHjelpemiddelsentralen: Boolean,
    val utleverttype: UtlevertTypeV2?,
    val overførtFraBruker: Brukernummer?,
    val annenKommentar: String?,
)

typealias Brukernummer = String

data class Opplysning(
    val ledetekst: LokalisertTekst,
    val innhold: List<Tekst>,
) {
    constructor(ledetekst: LokalisertTekst, innhold: Tekst) : this(ledetekst = ledetekst, innhold = listOf(innhold))

    constructor(ledetekst: LokalisertTekst, innhold: String) : this(ledetekst = ledetekst, innhold = Tekst(innhold))

    constructor(ledetekst: LokalisertTekst, innhold: LokalisertTekst) : this(
        ledetekst = ledetekst,
        innhold = Tekst(innhold),
    )
}

data class EnkelOpplysning(
    val ledetekst: LokalisertTekst,
    val innhold: LokalisertTekst,
)

data class Tekst(
    val fritekst: String? = null,
    val forhåndsdefinertTekst: LokalisertTekst? = null,
    val begrepsforklaring: LokalisertTekst? = null, // feks forklaring av "avlastningsbolig". Ikke relevant for fritekst.
) {
    constructor(forhåndsdefinertTekst: LokalisertTekst) : this(
        forhåndsdefinertTekst = forhåndsdefinertTekst,
        fritekst = null,
    )

    constructor(fritekst: String) : this(forhåndsdefinertTekst = null, fritekst = fritekst)
    constructor(nb: String, nn: String) : this(LokalisertTekst(nb = nb, nn = nn))

    init {
        require(
            (forhåndsdefinertTekst != null && fritekst == null) ||
                (forhåndsdefinertTekst == null && fritekst != null),
        ) { "Én, og bare én, av forhåndsdefinertTekst eller fritekst må ha verdi. Mottok forhåndsdefinertTekst <$forhåndsdefinertTekst> og fritekst <$fritekst>" }
    }
}

private val htmlPolicy = HtmlPolicyBuilder().allowElements("em", "strong").toFactory()

data class LokalisertTekst(
    val nb: String,
    val nn: String,
) {
    constructor(norsk: String) : this(nb = norsk, nn = norsk) // For enkle tekster som er like på begge målformer

    init {
        require(nb == htmlPolicy.sanitize(nb)) { "Ugyldig HTML i nb" }
        require(nn == htmlPolicy.sanitize(nn)) { "Ugyldig HTML i nn" }
    }
}

data class Varsel(
    val tekst: LokalisertTekst,
    val type: Varseltype,
)

enum class Varseltype {
    INFO,
    WARNING,
}

private fun tilPrioritet(hast: Hast?): Prioritet = if (hast != null) Prioritet.HAST else Prioritet.NORMAL
