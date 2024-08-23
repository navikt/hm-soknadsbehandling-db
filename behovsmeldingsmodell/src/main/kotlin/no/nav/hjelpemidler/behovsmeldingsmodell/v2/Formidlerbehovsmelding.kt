package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.Brukerkilde
import no.nav.hjelpemidler.behovsmeldingsmodell.BrukersituasjonVilkår
import no.nav.hjelpemidler.behovsmeldingsmodell.Bruksarena
import no.nav.hjelpemidler.behovsmeldingsmodell.FritakFraBegrunnelseÅrsak
import no.nav.hjelpemidler.behovsmeldingsmodell.Fødselsnummer
import no.nav.hjelpemidler.behovsmeldingsmodell.InnsenderRolle
import no.nav.hjelpemidler.behovsmeldingsmodell.Kontaktperson
import no.nav.hjelpemidler.behovsmeldingsmodell.LeveringTilleggsinfo
import no.nav.hjelpemidler.behovsmeldingsmodell.Oppfølgingsansvarlig
import no.nav.hjelpemidler.behovsmeldingsmodell.Personnavn
import no.nav.hjelpemidler.behovsmeldingsmodell.Prioritet
import no.nav.hjelpemidler.behovsmeldingsmodell.Signaturtype
import no.nav.hjelpemidler.behovsmeldingsmodell.Utleveringsmåte
import no.nav.hjelpemidler.behovsmeldingsmodell.UtlevertType
import no.nav.hjelpemidler.behovsmeldingsmodell.Veiadresse
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Bytte
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Godkjenningskurs
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Hast
import org.owasp.html.HtmlPolicyBuilder
import java.time.LocalDate
import java.util.UUID

data class Formidlerbehovsmelding(
    val bruker: Bruker,
    val brukersituasjon: Brukersituasjon,
    val hjelpemidler: Hjelpemidler,
    val levering: Levering,
    val innsender: Innsender,

    override val id: UUID,
    override val type: BehovsmeldingType,
    override val innsendingsdato: LocalDate,
    override val skjemaversjon: Int = 2,
    override val hjmBrukersFnr: Fødselsnummer = bruker.fnr,
    override val innsendersFnr: Fødselsnummer = innsender.fnr,
    override val prioritet: Prioritet = tilPrioritet(levering.hast),
) : BehovsmeldingBase

data class Bruker(
    val fnr: Fødselsnummer,
    val navn: Personnavn,
    val signaturtype: Signaturtype,
    val telefon: String,
    val veiadresse: Veiadresse?,
    val kommunenummer: String?,
    val brukernummer: String?,
    val kilde: Brukerkilde?,
    val erInformertOmRettigheter: Boolean?, // brukt i forbindelse med fritak fra fullmakt (covid)
)

data class Brukersituasjon(
    val bekreftedeVilkår: Set<BrukersituasjonVilkår>,
    val funksjonsnedsettelser: Set<Funksjonsnedsettelser>,
)

enum class Funksjonsnedsettelser {
    BEVEGELSE,
    KOGNISJON,
    HØRSEL,
}

data class Levering(
    val hjelpmiddelformidler: no.nav.hjelpemidler.behovsmeldingsmodell.v1.Levering.Hjelpemiddelformidler,

    val oppfølgingsansvarlig: Oppfølgingsansvarlig,
    val annenOppfølgingsansvarlig: no.nav.hjelpemidler.behovsmeldingsmodell.v1.Levering.AnnenOppfølgingsansvarlig?,

    /**
     * utleveringsmåte == null -> formidler har ikke fått spm om utlevering fordi det ikke er behov for denne infoen.
     * Skjer når hvert hjm. er markert som utlevert eller ikke trenger info om utlevering (feks for apper hvor lisens
     * sendes til MinSide på nav.no, eller til folkereg. adresse for barn under 18 år).
     */
    val utleveringsmåte: Utleveringsmåte?,
    val annenUtleveringsadresse: Veiadresse?,

    // utleveringKontaktperson == null => alle hjm. er allerede utlevert
    val utleveringKontaktperson: Kontaktperson?,
    val annenKontaktperson: no.nav.hjelpemidler.behovsmeldingsmodell.v1.Levering.AnnenKontaktperson?,

    val utleveringMerknad: String,

    val hast: Hast?,

    /**
     * Inneholder ekstra informasjon som automatisk er utledet. Dvs. det er ikke noe formidler har svart på (direkte).
     */
    val automatiskUtledetTilleggsinfo: Set<LeveringTilleggsinfo> = emptySet(),
)

data class Innsender(
    val fnr: Fødselsnummer,
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
    val bruksarena: Set<Bruksarena>,
    val utlevertinfo: Utlevertinfo,
    val opplysninger: List<Opplysning>,
    val varsler: List<Varsel>,
)

data class HjelpemiddelProdukt(
    val hmsArtNr: String,
    val artikkelnavn: String,
    val iso8: Iso8,
    val iso8Tittel: String,
    val rangering: Int,
    val delkontrakttittel: String,
    val sortimentkategori: String, // fra digithot-sortiment
)

data class Tilbehør(
    val hmsnr: String,
    val navn: String,
    val antall: Int,
    val begrunnelse: String?,
    val fritakFraBegrunnelseÅrsak: FritakFraBegrunnelseÅrsak?,
)

data class Utlevertinfo(
    val alleredeUtlevertFraHjelpemiddelsentralen: Boolean,
    val utleverttype: UtlevertType?,
    val overførtFraBruker: Brukernummer?,
    val annenKommentar: String?,
)

typealias Brukernummer = String

data class Opplysning(
    val ledetekst: I18n,
    val tekster: List<Tekst>, // TODO bedre navn enn tekst(er)?
) {
    constructor(ledetekst: I18n, tekst: Tekst) : this(ledetekst = ledetekst, tekster = listOf(tekst))
    constructor(ledetekst: I18n, tekst: I18n) : this(ledetekst = ledetekst, tekst = Tekst(tekst))
    constructor(ledetekst: I18n, tekst: String) : this(ledetekst = ledetekst, tekst = Tekst(tekst))
}

data class Tekst(
    val i18n: I18n? = null,
    val fritekst: String? = null,
    val begrepsforklaring: I18n? = null, // feks forklaring av "avlastningsbolig"
) {
    constructor(i18n: I18n) : this(i18n = i18n, fritekst = null)
    constructor(fritekst: String) : this(i18n = null, fritekst = fritekst)
    constructor(nb: String, nn: String) : this(I18n(nb = nb, nn = nn))

    init {
        require(
            (i18n != null && fritekst == null) ||
                (i18n == null && fritekst != null),
        ) { "Én, og bare én, av i18n eller fritekst må ha verdi. Mottok i18n <$i18n> og fritekst <$fritekst>" }
    }
}

private val htmlPolicy = HtmlPolicyBuilder().allowElements("em", "strong").toFactory()

data class I18n(
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
    val tekst: I18n,
    val type: Varseltype,
)

enum class Varseltype {
    INFO,
    WARNING,
}

private fun tilPrioritet(hast: Hast?): Prioritet = if (hast != null) Prioritet.HAST else Prioritet.NORMAL
