package no.nav.hjelpemidler.behovsmeldingsmodell

import com.fasterxml.jackson.annotation.JsonProperty

enum class BehovsmeldingType {
    SØKNAD,
    BESTILLING,
    BYTTE,
    BRUKERPASSBYTTE,
}

enum class BehovsmeldingStatus {
    VENTER_GODKJENNING,
    GODKJENT_MED_FULLMAKT,
    INNSENDT_FULLMAKT_IKKE_PÅKREVD,
    BRUKERPASSBYTTE_INNSENDT,
    GODKJENT,
    SLETTET,
    UTLØPT,
    ENDELIG_JOURNALFØRT,
    BESTILLING_FERDIGSTILT,
    BESTILLING_AVVIST,
    VEDTAKSRESULTAT_INNVILGET,
    VEDTAKSRESULTAT_MUNTLIG_INNVILGET,
    VEDTAKSRESULTAT_DELVIS_INNVILGET,
    VEDTAKSRESULTAT_AVSLÅTT,
    VEDTAKSRESULTAT_ANNET,
    UTSENDING_STARTET,
    VEDTAKSRESULTAT_HENLAGTBORTFALT,
    ;

    fun isSlettetEllerUtløpt(): Boolean = this == SLETTET || this == UTLØPT
}

enum class InnsenderRolle {
    FORMIDLER,
    BESTILLER,
}

enum class Signaturtype {
    BRUKER_BEKREFTER,
    FULLMAKT,
    FRITAK_FRA_FULLMAKT,
    IKKE_INNHENTET_FORDI_BYTTE,
    IKKE_INNHENTET_FORDI_BRUKERPASSBYTTE,
}

enum class Brukerkilde {
    PDL,
    FORMIDLER,
}

enum class BrukersituasjonVilkår {
    PRAKTISKE_PROBLEMER_I_DAGLIGLIVET_V1,
    VESENTLIG_OG_VARIG_NEDSATT_FUNKSJONSEVNE_V1,

    @JsonProperty("KAN_IKKE_LOESES_MED_ENKLERE_HJELPEMIDLER_V1")
    KAN_IKKE_LØSES_MED_ENKLERE_HJELPEMIDLER_V1,

    /**
     * NB! Det var en skrivefeil i denne, derfor @JsonProperty.
     */
    @JsonProperty("I_STAND_TIL_AA_BRUKE_HJELEPMIDLENE_V1")
    I_STAND_TIL_Å_BRUKE_HJELPEMIDLENE_V1,
}

enum class LeveringTilleggsinfo {
    UTLEVERING_KALENDERAPP,
    ALLE_HJELPEMIDLER_ER_UTLEVERT,
}

enum class Oppfølger {
    @JsonProperty("Hjelpemiddelformidler")
    HJELPEMIDDELFORMIDLER,

    @JsonProperty("NoenAndre")
    NOEN_ANDRE,
}

enum class Kontaktperson {
    @JsonProperty("Hjelpemiddelbruker")
    HJELPEMIDDELBRUKER,

    @JsonProperty("Hjelpemiddelformidler")
    HJELPEMIDDELFORMIDLER,

    @JsonProperty("AnnenKontaktperson")
    ANNEN_KONTAKTPERSON,
}

enum class Utleveringsmåte {
    @JsonProperty("FolkeregistrertAdresse")
    FOLKEREGISTRERT_ADRESSE,

    @JsonProperty("AnnenBruksadresse")
    ANNEN_BRUKSADRESSE,

    @JsonProperty("Hjelpemiddelsentralen")
    HJELPEMIDDELSENTRALEN,

    @Deprecated("Bruke ikke lenger")
    @JsonProperty("AlleredeUtlevertAvNav")
    ALLEREDE_UTLEVERT_AV_NAV,
}

enum class Hasteårsak {
    HINDRE_VIDERE_UTVIKLING_AV_TRYKKSÅR,
    BEHANDLE_ELLER_HINDRE_VIDERE_UTVIKLING_AV_TRYKKSÅR,
    TERMINALFASE,
}

enum class Bruksarena {
    EGET_HJEM,
    EGET_HJEM_IKKE_AVLASTNING,
    OMSORGSBOLIG_BOFELLESKAP_SERVICEBOLIG,
    BARNEHAGE,

    @JsonProperty("GRUNN_ELLER_VIDEREGÅENDESKOLE")
    GRUNN_ELLER_VIDEREGÅENDE_SKOLE,
    SKOLEFRITIDSORDNING,
    INSTITUSJON,
    INSTITUSJON_BARNEBOLIG,
    INSTITUSJON_BARNEBOLIG_IKKE_PERSONLIG_BRUK,
}

enum class BytteÅrsak {
    UTSLITT,
    VOKST_FRA,
    ENDRINGER_I_INNBYGGERS_FUNKSJON,
    FEIL_STØRRELSE,
    VURDERT_SOM_ØDELAGT_AV_LOKAL_TEKNIKER,
}

enum class OppreisningsstolLøftType {
    SKRÅLØFT,
    RETTLØFT,
}

enum class OppreisningsstolBruksområde {
    EGEN_BOENHET,
    FELLESAREAL,
}

enum class OppreisningsstolBehov {
    OPPGAVER_I_DAGLIGLIVET,
    PLEID_I_HJEMMET,
    FLYTTE_MELLOM_STOL_OG_RULLESTOL,
}

enum class SidebetjeningspanelPosisjon {
    HØYRE,
    VENSTRE,
}

enum class PosisjoneringsputeForBarnBruk {
    TILRETTELEGGE_UTGANGSSTILLING,
    TRENING_AKTIVITET_STIMULERING,
}

enum class PosisjoneringsputeBehov {
    STORE_LAMMELSER,
    DIREKTE_AVHJELPE_I_DAGLIGLIVET,
}

enum class PosisjoneringsputeOppgaverIDagligliv {
    SPISE_DRIKKE_OL,
    BRUKE_DATAUTSTYR,
    FØLGE_OPP_BARN,
    HOBBY_FRITID_U26,
    ANNET,
}

enum class BruksområdeGanghjelpemiddel {
    TIL_FORFLYTNING,
    TIL_TRENING_OG_ANNET,
}

enum class GanghjelpemiddelType {
    GÅBORD,
    SPARKESYKKEL,
    KRYKKE,
    GÅTRENING,
    GÅSTOL,
}

enum class PlasseringType {
    @JsonProperty("Venstre")
    VENSTRE,

    @JsonProperty("Høyre")
    HØYRE,
}

enum class UtlevertType {
    @JsonProperty("FremskuttLager")
    FREMSKUTT_LAGER,

    @JsonProperty("Korttidslån")
    KORTTIDSLÅN,

    @JsonProperty("Overført")
    OVERFØRT,

    @JsonProperty("Annet")
    ANNET,
}

enum class SitteputeValg {
    @JsonProperty("TrengerSittepute")
    TRENGER_SITTEPUTE,

    @JsonProperty("HarFraFor")
    HAR_FRA_FØR,
}

enum class MadrassValg {
    @JsonProperty("TrengerMadrass")
    TRENGER_MADRASS,

    @JsonProperty("HarFraFor")
    HAR_FRA_FØR,
}

enum class AutomatiskGenerertTilbehør {
    @JsonProperty("Sittepute")
    SITTEPUTE,
}
