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
import no.nav.hjelpemidler.behovsmeldingsmodell.Personnavn
import no.nav.hjelpemidler.behovsmeldingsmodell.Signaturtype
import no.nav.hjelpemidler.behovsmeldingsmodell.Veiadresse
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Bytte
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Godkjenningskurs
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Hast
import java.time.LocalDate
import java.util.UUID

class Formidlerbehovsmelding(
    id: UUID,
    type: BehovsmeldingType,
    innsendingsdato: LocalDate,

    val bruker: Bruker,
    val brukersituasjon: Brukersituasjon,
    val hjelpemidler: Hjelpemidler,
    val levering: Levering,
    val innsender: Innsender,
) : Behovsmelding(
    id = id,
    type = type,
    innsendingsdato = innsendingsdato,
    prioritet = tilPrioritet(levering.hast),
    hjmBrukersFnr = bruker.fnr,
    innsendersFnr = innsender.fnr,
)

data class Bruker(
    val fnr: Fødselsnummer,
    val navn: Personnavn,
    val signaturtype: Signaturtype,
    val telefon: String,
    val veiadresse: Veiadresse?,
    val kommunenummer: String?,
    val brukernummer: String?,
    val kilde: Brukerkilde?,

    // val kroppsmål: Kroppsmål?, // TODO kun som info på relevenate hjm.
    // val erInformertOmRettigheter: Boolean?, // TODO brukersituasjon???
    // val borIPilotkommuneForHast: Boolean? = false, // TODO brukes kun i hm-soknad-api for statistikk
)

data class Brukersituasjon(
    val bekreftedeVilkår: Set<BrukersituasjonVilkår>,
    val funksjonsnedsettelser: Funksjonsnedsettelser,
)

data class Funksjonsnedsettelser(
    val bevegelse: Boolean,
    val kognisjon: Boolean,
    val hørsel: Boolean,
)

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
    // val organisasjoner: List<Organisasjon>, // TODO hva brukes denne til?
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
    val opplysninger: List<Opplysning>,
    val varsler: List<I18n>,

//    val beskrivelse: String, // TODO hva er dette?
)

data class HjelpemiddelProdukt(
    val hmsnr: String,
    val navn: String,
    val iso8: Iso8,
    val iso8Navn: String,
    val rangering: Int,
    val delkontrakttittel: String,
)

data class Tilbehør(
    val hmsnr: String,
    val navn: String,
    val antall: Int,
    val begrunnelse: String?,
    val fritakFraBegrunnelseÅrsak: FritakFraBegrunnelseÅrsak?,
)

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

data class I18n(
    val nb: String,
    val nn: String,
) {
    constructor(norsk: String) : this(nb = norsk, nn = norsk) // For enkle tekster som er like på begge målformer
}

private fun tilPrioritet(hast: Hast?): Prioritet = if (hast != null) Prioritet.HAST else Prioritet.NORMAL
