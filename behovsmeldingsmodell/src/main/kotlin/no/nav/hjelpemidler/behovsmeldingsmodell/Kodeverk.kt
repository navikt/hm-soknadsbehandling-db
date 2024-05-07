package no.nav.hjelpemidler.behovsmeldingsmodell

enum class BehovsmeldingType {
    SØKNAD,
    BESTILLING,
    BYTTE,
    BRUKERPASSBYTTE,
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
    KAN_IKKE_LOESES_MED_ENKLERE_HJELPEMIDLER_V1,
    I_STAND_TIL_AA_BRUKE_HJELEPMIDLENE_V1,
}

enum class LeveringTilleggsinfo {
    UTLEVERING_KALENDERAPP,
    ALLE_HJELPEMIDLER_ER_UTLEVERT,
}

enum class Oppfølger {
    Hjelpemiddelformidler,
    NoenAndre,
}

enum class Kontaktperson {
    Hjelpemiddelbruker,
    Hjelpemiddelformidler,
    AnnenKontaktperson,
}

enum class Utleveringsmåte {
    FolkeregistrertAdresse,
    AnnenBruksadresse,
    Hjelpemiddelsentralen,

    @Deprecated("Bruke ikke lenger")
    AlleredeUtlevertAvNav,
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
    GRUNN_ELLER_VIDEREGÅENDESKOLE,
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
    Venstre,
    Høyre,
}

enum class UtlevertType {
    FremskuttLager,
    Korttidslån,
    Overført,
    Annet,
}

enum class SitteputeValg {
    TrengerSittepute,
    HarFraFor,
}

enum class MadrassValg {
    TrengerMadrass,
    HarFraFor,
}

enum class AutomatiskGenerertTilbehør {
    Sittepute,
}
