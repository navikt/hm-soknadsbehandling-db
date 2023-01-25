package no.nav.hjelpemidler.soknad.db.domain.kommune_api

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
    val soknad: Soknad
) {
    fun filtrerForKommuneApiet() = this.copy(
        bestillingsordningsjekk = null,
        soknad = this.soknad.copy(innsender = null)
    )

    companion object {
        private val specializedObjectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Skal feile hvis man har ukjente verdier i JsonNode, da må denne vedlikeholdes og hva som deles med
            // kommunen revurderes!
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        fun fraJsonNode(node: JsonNode): Behovsmelding {
            return kotlin.runCatching {
                specializedObjectMapper.readValue<Behovsmelding>(node.toString())
            }.getOrElse { cause ->
                throw RuntimeException("Kunne ikke opprette Behovsmelding-typen fra JsonNode: ${cause.message}", cause)
            }
        }
    }
}

enum class BehovsmeldingType {
    SØKNAD, BESTILLING
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
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Innsender(
    val godkjenningskurs: List<GodkjenningsKurs>?,
    val organisasjoner: List<Organisasjon>?,
    val somRolle: InnsenderRolle
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Organisasjon(
    val orgnr: String,
    val navn: String,
    val orgform: String = "",
    val overordnetOrgnr: String? = null,
    val kommunenummer: String? = null,
)

enum class InnsenderRolle {
    FORMIDLER, BESTILLER
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
)

@JsonInclude(JsonInclude.Include.NON_NULL)
class Fødselsnummer(@get:JsonValue val value: String) {

    private val elevenDigits = Regex("\\d{11}")

    init {
        if (!elevenDigits.matches(value)) {
            throw IllegalArgumentException("$value is not a valid fnr")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Fødselsnummer) return false

        if (value != other.value) return false
        return true
    }
}

enum class Signaturtype {
    BRUKER_BEKREFTER, FULLMAKT, FRITAK_FRA_FULLMAKT
}

enum class BrukerKilde {
    PDL, FORMIDLER
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Kroppsmaal(
    val setebredde: Int?,
    val laarlengde: Int?,
    val legglengde: Int?,
    val hoyde: Int?,
    val kroppsvekt: Int?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Brukersituasjon(
    val bostedRadioButton: String?,
    val bruksarenaErDagliglivet: Boolean,
    val nedsattFunksjon: Boolean,
    val nedsattFunksjonTypes: NedsattFunksjonTypes?,
    val storreBehov: Boolean,
    val praktiskeProblem: Boolean,
    val skalIkkeBrukesTilAndreFormaal: Boolean? // Kun for bestiller
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NedsattFunksjonTypes(
    val bevegelse: Boolean,
    val kognisjon: Boolean,
    val horsel: Boolean
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Hjelpemidler(
    val hjelpemiddelListe: List<HjelpemiddelItem>,
    val hjelpemiddelTotaltAntall: Int
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
    val hmfTreffesEnklest: String
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
    val utleveringsmaateRadioButton: UtleveringsMaate
)

enum class Oppfoelger {
    Hjelpemiddelformidler, NoenAndre
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
    val elektriskRullestolInfo: ElektriskRullestolInfo?
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
    var paakrevdGodkjenningskurs: PaakrevdGodkjenningsKurs?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Techdata(
    val techlabeldk: String?,
    val datavalue: String?,
    val techdataunit: String?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaakrevdGodkjenningsKurs(
    val kursId: Int?,
    val tittel: String?,
    val isokode: String?,
    val formidlersGjennomforing: FormidlersGjennomforingAvKurs?
)

enum class FormidlersGjennomforingAvKurs {
    GODKJENNINGSKURS_DB, VALGT_AV_FORMIDLER
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
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonlofterInfo(
    val harBehovForSeilEllerSele: Boolean
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RullestolInfo(
    val skalBrukesIBil: Boolean?,
    val sitteputeValg: SitteputeValg?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtlevertInfo(
    val utlevertType: UtlevertType?,
    val overførtFraBruker: String?,
    val annenKommentar: String?
)

enum class PlasseringType {
    Venstre, Høyre
}

enum class UtlevertType {
    FremskuttLager,
    Korttidslån,
    Overført,
    Annet
}

enum class SitteputeValg {
    TrengerSittepute, HarFraFor
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Tilbehor(
    val hmsnr: String,
    val antall: Int?,
    val navn: String,
    val automatiskGenerert: AutomatiskGenerertTilbehor?,
    val brukAvForslagsmotoren: BrukAvForslagsmotoren?,
)

enum class AutomatiskGenerertTilbehor {
    Sittepute
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
    val tilleggsinfo: String?
)
