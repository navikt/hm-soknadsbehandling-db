package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.AutomatiskGenerertTilbehør
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Brukerkilde
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.BrukersituasjonVilkår
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Bytte
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.BytteÅrsak
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Fødselsnummer
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Hasteårsak
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.HjelpemiddelProdukt
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.InnsenderRolle
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Kontaktperson
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.LeveringTilleggsinfo
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Personnavn
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Signaturtype
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Tilbehør
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.UtlevertInfo
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Veiadresse
import java.time.LocalDate
import java.util.UUID

// TODO Hva er en bra navngiving? TerapeutBehovsmelding/InnsenderBehovsmelding/PåVegneAvBehovsmelding/...
class FormidlerBehovsmelding(
    id: UUID,
    type: BehovsmeldingType,
    innsendingsdato: LocalDate,

    val bruker: Bruker,
    val brukersituasjon: Brukersituasjon,
    val hjelpemidler: Hjelpemidler,
    val levering: Levering,
    val innsender: Innsender?,
    val hast: Hast?,
) : Behovsmelding(id, type, innsendingsdato, prioritet(hast))

/*
 TODO hm-soknad-api kan ha en wrapper:
 data class BehovsmeldingRequest (
    val statistikkdata: Statistikkdata,
    val behovsmelding: FormidlerBehovsmelding,
 )

 data class Statistikkdata(
    val innsenderOrganisasjoner: List<Organisasjon>,
    val innsendersGodkjenningskurs: List<Godkjenningskurs>,
    val brukerBorIPilotkommuneForHast: Boolean,
)
 for å skille på permanent behovsmelding som vi vil ha, og andre midlertidige data som kun er til statistikk o.l.
 */


data class Innsender(
    val rolle: InnsenderRolle, // hotsak greit å vite
    val tjenestligeBehovForUtlånsoversikt: Set<Iso6>, //kun statistikk i dag, men kanskje vise for bruker/i hotsak i fremtiden
)

data class Bruker(
    val fnr: Fødselsnummer,
    val navn: Personnavn,
    val signaturtype: Signaturtype,
    val telefon: String,
    val veiadresse: Veiadresse,
    val kommunenummer: String?,
    val brukernummer: String?,
    val kilde: Brukerkilde?,
)


data class Brukersituasjon(
    val bekreftedeVilkår: Set<BrukersituasjonVilkår>,
    val funksjonsnedsettelser: List<Funksjonsnedsettelse>,
)

data class Hjelpemidler(
    val hjelpemidler: List<Hjelpemiddel>,
    val totaltAntall: Int,
) {
    @JsonIgnore
    val påkrevdeGodkjenningskurs: List<HjelpemiddelProdukt.PåkrevdGodkjenningskurs> = hjelpemidler
        .mapNotNull { it.produkt?.påkrevdGodkjenningskurs }
        .distinctBy { it.kursId }
        .sortedBy { it.tittel }
}

data class Levering(
    val hjelpemiddelformidler: Hjelpemiddelformidler,

    val oppfølgingsansvarlig: Oppfølgingsansvarlig,
    val annenOppfølgingsansvarlig: AnnenOppfølgingsansvarlig?,

    // utleveringsmåte == null => alle hjm. er allerede utlevert
    val utleveringsmåte: Utleveringsmåte?,
    val annenUtleveringsadresse: Veiadresse?,

    // utleveringKontaktperson == null => alle hjm. er allerede utlevert
    val utleveringKontaktperson: Kontaktperson?,
    val annenKontaktperson: AnnenKontaktperson?,

    val utleveringMerknad: String,

    // TODO dette er (per i dag) ekstra info som automatisk blir lagt på av behovsmeldingen, og ikke fylt ut av innsender.
    // burde vi hatt et bedre navn på det? automatiskTilleggsinfo/metainfo eller noe sånnt?
    val tilleggsinfo: Set<LeveringTilleggsinfo> = emptySet(),
) {

    init {
        if (oppfølgingsansvarlig == Oppfølgingsansvarlig.ANNEN_OPPFØLGINGSANSVARLIG) {
            require(annenOppfølgingsansvarlig != null) { "annenOppfølgingsansvarlig må være satt for oppfølgingsansvarlig $oppfølgingsansvarlig" }
        }
        if (utleveringsmåte == Utleveringsmåte.ANNEN_BRUKSADRESSE) {
            require(annenUtleveringsadresse != null) { "annenUtleveringsadresse må være satt for utleveringsmåte $utleveringsmåte" }
        }
        if (utleveringKontaktperson == Kontaktperson.ANNEN_KONTAKTPERSON) {
            require(annenKontaktperson != null) { "annenKontaktperson må være satt for utleveringKontaktperson $utleveringKontaktperson" }
        }
    }

    data class Hjelpemiddelformidler(
        val navn: Personnavn,
        val arbeidssted: String,
        val stilling: String,
        val telefon: String,
        val adresse: Veiadresse,
        val epost: String,
        val treffesEnklest: String,
    )

    data class AnnenOppfølgingsansvarlig(
        val navn: Personnavn,
        val arbeidssted: String,
        val stilling: String,
        val telefon: String,
        val ansvarFor: String,
    )

    data class AnnenKontaktperson(
        val navn: Personnavn,
        val telefon: String,
    )
}

data class Hast(
    val årsaker: Set<Hasteårsak>,
    val begrunnelse: String?,
)

private fun prioritet(hast: Hast?): Prioritet = if (hast != null) Prioritet.HAST else Prioritet.NORMAL

data class Hjelpemiddel(
    val antall: Int,
    val beskrivelse: String,
    val hjelpemiddelkategori: String,
    val hmsnr: String,
    val uniqueKey: String,
    val utlevertFraHjelpemiddelsentralen: Boolean,
    val utlevertInfo: UtlevertInfo? = null,
    val tilbehør: List<Tilbehør>? = null,
    val navn: String? = null,
    val produkt: HjelpemiddelProdukt? = null,
    val bytter: List<Bytte> = emptyList(),
    val opplysninger: Hjelpemiddelopplysninger,
)

data class Bytte(
    val erTilsvarende: Boolean,
    val hmsnr: String,
    val serienr: String? = null,
    val hjmNavn: String,
    val hjmKategori: String,
    val årsak: BytteÅrsak? = null,
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
