package no.nav.hjelpemidler.soknad.db.domain.kommuneapi.v2

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hjelpemidler.configuration.Environment
import java.time.Instant
import java.time.LocalDate
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

interface BehovsmeldingBase {
    val id: UUID
    val type: BehovsmeldingType
    val innsendingsdato: LocalDate
    val innsendingstidspunkt: Instant?
    val prioritet: Prioritet
    val hjmBrukersFnr: Fødselsnummer
    val skjemaversjon: Int
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Innsenderbehovsmelding(
    val bruker: Bruker,
    val brukersituasjon: Brukersituasjon,
    val hjelpemidler: Hjelpemidler,
    val levering: Levering,
    val innsender: Innsender?,
    val vedlegg: List<Vedlegg> = emptyList(),

    val metadata: InnsenderbehovsmeldingMetadata?,

    override val id: UUID,
    override val type: BehovsmeldingType,
    override val innsendingsdato: LocalDate,
    override val innsendingstidspunkt: Instant? = null,
    override val skjemaversjon: Int,
    override val hjmBrukersFnr: Fødselsnummer,
    override val prioritet: Prioritet,
) : BehovsmeldingBase {
    fun filtrerForKommuneApiet() = this.copy(
        metadata = null,
        innsender = null,
    )

    companion object {
        private val specializedObjectMapper: JsonMapper = jacksonMapperBuilder()
            .addModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Skal feile hvis man har ukjente verdier i JsonNode, da må denne vedlikeholdes og hva som deles med
            // kommunen revurderes!
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION, !Environment.current.tier.isProd)
            .build()

        fun fraJsonNode(node: JsonNode): Innsenderbehovsmelding {
            return runCatching {
                specializedObjectMapper.readValue<Innsenderbehovsmelding>(node.toString())
            }.getOrElse { cause ->
                throw RuntimeException("Kunne ikke opprette Innsenderbehovsmelding-typen fra JsonNode: ${cause.message}", cause)
            }
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Vedlegg(
    val id: UUID,
    val navn: String,
    val type: VedleggType,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class VedleggType {
    LEGEERKLÆRING_FOR_VARMEHJELPEMIDDEL,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InnsenderbehovsmeldingMetadata(
    val bestillingsordningsjekk: Bestillingsordningsjekk?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Bruker(
    val fnr: Fødselsnummer,
    val navn: Personnavn,
    val signaturtype: Signaturtype,
    val telefon: String?,
    val veiadresse: Veiadresse?,
    val kommunenummer: String?,
    val brukernummer: String?,
    val kilde: Brukerkilde?,
    val legacyopplysninger: List<EnkelOpplysning>, // for visning av opplysninger som bare finnes i eldre behovsmeldinger
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Brukersituasjon(
    val vilkår: Set<BrukersituasjonVilkårV2>,
    val funksjonsnedsettelser: Set<Funksjonsnedsettelser>,
    val funksjonsbeskrivelse: Funksjonsbeskrivelse?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BrukersituasjonVilkårV2(
    val vilkårtype: BrukersituasjonVilkårtype,
    val tekst: LokalisertTekst,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Hjelpemiddelformidler(
    val navn: Personnavn,
    val arbeidssted: String,
    val stilling: String,
    val telefon: String,
    val adresse: Veiadresse,
    val epost: String,
    val treffesEnklest: String,
    val kommunenavn: String?,
    val kommunenummer: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AnnenOppfølgingsansvarlig(
    val navn: Personnavn,
    val arbeidssted: String,
    val stilling: String,
    val telefon: String,
    val ansvarFor: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AnnenKontaktperson(
    val navn: Personnavn,
    val telefon: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Levering(
    val hjelpemiddelformidler: Hjelpemiddelformidler,

    val oppfølgingsansvarlig: OppfølgingsansvarligV2,
    val annenOppfølgingsansvarlig: AnnenOppfølgingsansvarlig?,

    /**
     * utleveringsmåte == null -> formidler har ikke fått spm om utlevering fordi det ikke er behov for denne infoen.
     * Skjer når hvert hjm. er markert som utlevert eller ikke trenger info om utlevering (feks for apper hvor lisens
     * sendes til MinSide på nav.no, eller til folkereg. adresse for barn under 18 år).
     */
    val utleveringsmåte: UtleveringsmåteV2?,
    val annenUtleveringsadresse: Veiadresse?,

    // utleveringKontaktperson == null => alle hjm. er allerede utlevert
    val utleveringKontaktperson: KontaktpersonV2?,
    val annenKontaktperson: AnnenKontaktperson?,

    val utleveringMerknad: String,

    val hast: Hast?,

    /**
     * Inneholder ekstra informasjon som automatisk er utledet. Dvs. det er ikke noe formidler har svart på (direkte).
     */
    val automatiskUtledetTilleggsinfo: Set<LeveringTilleggsinfo> = emptySet(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Innsender(
    val rolle: InnsenderRolle,
    val erKommunaltAnsatt: Boolean?,
    val kurs: List<Godkjenningskurs>,
    val sjekketUtlånsoversiktForKategorier: Set<String>?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Hjelpemidler(
    val hjelpemidler: List<Hjelpemiddel>,
    val tilbehør: List<Tilbehør>? = emptyList(),
    val totaltAntall: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Hjelpemiddel(
    /**
     * Tilfeldig genrerert id for å unikt kunne identifisere hjelpemidler,
     * feks. dersom det er lagt til flere innslag med samme hmsArtNr.
     * For gamle saker: hjelpemiddelId = hjelpemiddel.produkt.stockid + new Date().getTime()
     * For nye saker (etter ca 2024-11-05): hjelpemiddelId = UUID()
     */
    val hjelpemiddelId: String,
    val antall: Int,
    val produkt: HjelpemiddelProdukt,
    val tilbehør: List<Tilbehør>,
    val bytter: List<Bytte>,
    val bruksarenaer: Set<BruksarenaV2>,
    val utlevertinfo: Utlevertinfo,
    val opplysninger: List<Opplysning>,
    val varsler: List<Varsel>,
    val saksbehandlingvarsel: List<Varsel> = emptyList(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HjelpemiddelProdukt(
    val hmsArtNr: String,
    val artikkelnavn: String,
    val iso8: String,
    val iso8Tittel: String,
    val delkontrakttittel: String,
    val sortimentkategori: String, // fra digithot-sortiment
    val delkontraktId: String?, // Brukt av hm-saksfordeling for å sortere til Gosys.

    /*
    null -> ikke på rammeavtale
    Har i sjeldne tilfeller skjedd at formidler får søkt om produkt som ikke lenger er på rammeavtale, antageligvis pga
    endring i produkter på rammeavtale etter lansering av rammeavtalen.
     */
    val rangering: Int?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Tilbehør(
    val tilbehørId: UUID? = null,
    val hmsArtNr: String,
    val navn: String,
    val antall: Int,
    val begrunnelse: String?,
    val fritakFraBegrunnelseÅrsak: FritakFraBegrunnelseÅrsak?,
    val skalBrukesMed: SkalBrukesMed?, // For frittstående tilbehør
    val opplysninger: List<Opplysning>?,
    val saksbehandlingvarsel: List<Varsel>?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SkalBrukesMed(
    val type: SkalBrukesMedType,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class SkalBrukesMedType {
    HJELPEMIDDEL_I_INNSENDT_SAK,
    HJELPEMIDDEL_I_UTLÅN,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Utlevertinfo(
    val alleredeUtlevertFraHjelpemiddelsentralen: Boolean,
    val utleverttype: UtlevertTypeV2?,
    val overførtFraBruker: String?,
    val annenKommentar: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Opplysning(
    val ledetekst: LokalisertTekst,
    val innhold: List<Tekst>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnkelOpplysning(
    val ledetekst: LokalisertTekst,
    val innhold: LokalisertTekst,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Tekst(
    val fritekst: String? = null,
    val forhåndsdefinertTekst: LokalisertTekst? = null,
    val begrepsforklaring: LokalisertTekst? = null, // feks forklaring av "avlastningsbolig". Ikke relevant for fritekst.
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LokalisertTekst(
    val nb: String,
    val nn: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Varsel(
    val tekst: LokalisertTekst,
    val type: Varseltype,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class Varseltype {
    INFO,
    WARNING,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Fødselsnummer(@get:JsonValue val value: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Personnavn(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Veiadresse(
    val adresse: String,
    val postnummer: String,
    val poststed: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class Hasteårsak {
    UTVIKLING_AV_TRYKKSÅR,
    TERMINALPLEIE,

    @Deprecated("Erstattet av _V2")
    UTSKRIVING_FRA_SYKEHUS_SOM_IKKE_KAN_PLANLEGGES,

    @Deprecated("Erstattet av _V3")
    UTSKRIVING_FRA_SYKEHUS_SOM_IKKE_KAN_PLANLEGGES_V2,
    UTSKRIVING_FRA_SYKEHUS_SOM_IKKE_KAN_PLANLEGGES_V3,
    RASK_FORVERRING_AV_ALVORLIG_DIAGNOSE,
    ANNET,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Hast(
    val hasteårsaker: Set<Hasteårsak>,
    val hastBegrunnelse: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Godkjenningskurs(
    val id: Int,
    val title: String,
    val kilde: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class InnbyggersVarigeFunksjonsnedsettelse {
    ALDERDOMSSVEKKELSE,
    ANNEN_VARIG_DIAGNOSE,
    ANNEN_DIAGNOSE,
    UAVKLART,
    UAVKLART_V2,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Funksjonsbeskrivelse(
    val innbyggersVarigeFunksjonsnedsettelse: InnbyggersVarigeFunksjonsnedsettelse,
    val diagnose: String?,
    val beskrivelse: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class BytteÅrsak {
    UTSLITT,
    VOKST_FRA,
    ENDRINGER_I_INNBYGGERS_FUNKSJON,
    FEIL_STØRRELSE,
    VURDERT_SOM_ØDELAGT_AV_LOKAL_TEKNIKER,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Bytte(
    val erTilsvarende: Boolean,
    val hmsnr: String,
    val serienr: String? = null,
    val hjmNavn: String,
    val hjmKategori: String,
    val årsak: BytteÅrsak? = null,
    val versjon: String = "v1",
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Bestillingsordningsjekk(
    val kanVæreBestilling: Boolean,
    val kriterier: Kriterier,
    val metaInfo: MetaInfo,
    val version: String,
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Kriterier(
        @JsonProperty("alleHovedProdukterPåBestillingsOrdning")
        val alleHovedprodukterPåBestillingsordning: Boolean,
        @JsonProperty("alleTilbehørPåBestillingsOrdning")
        val alleTilbehørPåBestillingsordning: Boolean,
        val brukerHarHjelpemidlerFraFør: Boolean? = null,
        val brukerHarInfotrygdVedtakFraFør: Boolean? = null,
        val brukerHarHotsakVedtakFraFør: Boolean? = null,
        val leveringTilFolkeregistrertAdresse: Boolean,
        val brukersAdresseErSatt: Boolean,
        val brukerBorIkkeIUtlandet: Boolean,
        val brukerErIkkeSkjermetPerson: Boolean,
        val inneholderIkkeFritekst: Boolean,
        val kildeErPdl: Boolean,
        val harIkkeForMangeOrdrelinjer: Boolean,
        val ingenProdukterErAlleredeUtlevert: Boolean,
        val brukerErTilknyttetBydelIOslo: Boolean?,
        val harIngenBytter: Boolean,
        val brukerHarAdresseIOeBS: Boolean,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class MetaInfo(
        @JsonProperty("hovedProdukter")
        val hovedprodukter: List<String>,
        @JsonProperty("hovedProdukterIkkePåBestillingsordning")
        val hovedprodukterIkkePåBestillingsordning: List<String>,
        val tilbehør: List<String>,
        val tilbehørIkkePåBestillingsordning: List<String>,
    )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class UtlevertTypeV2 {
    FREMSKUTT_LAGER,
    KORTTIDSLÅN,
    OVERFØRT,
    ANNET,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class UtleveringsmåteV2 {
    FOLKEREGISTRERT_ADRESSE,
    ANNEN_BRUKSADRESSE,
    HJELPEMIDDELSENTRALEN,

    @Deprecated("Brukes ikke i digital behovsmelding lenger")
    ALLEREDE_UTLEVERT_AV_NAV,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class Signaturtype {
    BRUKER_BEKREFTER,
    FULLMAKT,
    FRITAK_FRA_FULLMAKT,
    IKKE_INNHENTET_FORDI_BYTTE,
    IKKE_INNHENTET_FORDI_BRUKERPASSBYTTE,
    IKKE_INNHENTET_FORDI_KUN_TILBEHØR,
    IKKE_INNHENTET_FORDI_KUN_TILBEHØR_V2,
    IKKE_INNHENTET_FORDI_KUN_TILBEHØR_V3,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class Prioritet {
    NORMAL,
    HAST,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class OppfølgingsansvarligV2 {
    HJELPEMIDDELFORMIDLER,
    ANNEN_OPPFØLGINGSANSVARLIG,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class LeveringTilleggsinfo {
    UTLEVERING_KALENDERAPP,
    ALLE_HJELPEMIDLER_ER_UTLEVERT,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class KontaktpersonV2 {
    HJELPEMIDDELBRUKER,
    HJELPEMIDDELFORMIDLER,
    ANNEN_KONTAKTPERSON,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class InnsenderRolle {
    FORMIDLER,
    BESTILLER,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class Funksjonsnedsettelser {
    BEVEGELSE,
    KOGNISJON,
    HØRSEL,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class FritakFraBegrunnelseÅrsak {
    ER_PÅ_BESTILLINGSORDNING,
    IKKE_I_PILOT,
    ER_SELVFORKLARENDE_TILBEHØR,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class BruksarenaV2 {
    EGET_HJEM,
    EGET_HJEM_IKKE_AVLASTNING,
    OMSORGSBOLIG_BOFELLESKAP_SERVICEBOLIG,
    BARNEHAGE,
    GRUNN_ELLER_VIDEREGÅENDE_SKOLE,
    SKOLEFRITIDSORDNING,
    INSTITUSJON,
    INSTITUSJON_BARNEBOLIG,
    INSTITUSJON_BARNEBOLIG_KUN_PERSONLIG_BRUK,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class BrukersituasjonVilkårtype {
    @Deprecated("Ikke lenger et valg i hm-soknad")
    NEDSATT_FUNKSJON,

    @Deprecated("Ikke lenger et valg i hm-soknad")
    STØRRE_BEHOV,

    @Deprecated("Ikke lenger et valg i hm-soknad")
    PRAKTISKE_PROBLEM,

    PRAKTISKE_PROBLEMER_I_DAGLIGLIVET_V1,
    VESENTLIG_OG_VARIG_NEDSATT_FUNKSJONSEVNE_V1,
    KAN_IKKE_LØSES_MED_ENKLERE_HJELPEMIDLER_V1,
    I_STAND_TIL_Å_BRUKE_HJELPEMIDLENE_V1,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class BehovsmeldingType {
    SØKNAD,
    BESTILLING,
    BYTTE,
    BRUKERPASSBYTTE,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class Brukerkilde {
    PDL,
    FORMIDLER,
}
