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
    val hast: Hast?,
) : Behovsmelding(
    id = id,
    type = type,
    innsendingsdato = innsendingsdato,
    prioritet = toPrioritet(hast),
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

    // utleveringsmåte == null => alle hjm. er allerede utlevert // TODO null her og UTLEVERT i tilleggsinfo, eller kun ALLEREDE_UTLEVERT HER?
    val utleveringsmåte: Utleveringsmåte?,
    val annenUtleveringsadresse: Veiadresse?,

    // utleveringKontaktperson == null => alle hjm. er allerede utlevert
    val utleveringKontaktperson: Kontaktperson?,
    val annenKontaktperson: no.nav.hjelpemidler.behovsmeldingsmodell.v1.Levering.AnnenKontaktperson?,

    val utleveringMerknad: String,

    // TODO dette er (per i dag) ekstra info som automatisk blir lagt på av behovsmeldingen, og ikke fylt ut av innsender.
    // burde vi hatt et bedre navn på det? automatiskTilleggsinfo/metainfo eller noe sånnt?
    val tilleggsinfo: Set<LeveringTilleggsinfo> = emptySet(),
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
    val label: I18n,
    val tekster: List<Tekst>, // TODO bedre navn enn tekst(er)?
) {
    constructor(label: I18n, tekst: Tekst) : this(label = label, tekster = listOf(tekst))
    constructor(label: I18n, tekst: I18n) : this(label = label, tekst = Tekst(tekst))
    constructor(label: I18n, tekst: String) : this(label = label, tekst = Tekst(tekst))
}

data class Tekst(
    val i18n: I18n? = null,
    val fritekst: String? = null,
) {
    constructor(i18n: I18n) : this(i18n = i18n, fritekst = null)
    constructor(fritekst: String) : this(i18n = null, fritekst = fritekst)
}

data class I18n(
    val nb: String,
    val nn: String = nb,
)

private fun toPrioritet(hast: Hast?): Prioritet = if (hast != null) Prioritet.HAST else Prioritet.NORMAL
