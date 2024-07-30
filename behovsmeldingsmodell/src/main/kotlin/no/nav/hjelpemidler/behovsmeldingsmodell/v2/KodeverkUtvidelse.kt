package no.nav.hjelpemidler.behovsmeldingsmodell.v2

/**
 * Lagt i egen klasse inntil videre for å skille mellom eksisterende og nye verdier under utarbeiding.
 */

enum class Prioritet {
    NORMAL,
    HAST,
}

enum class Funksjonsnedsettelse {
    BEVEGELSE,
    KOGNISJON,
    HØRSEL,
}

/**
 * Her kan det potensielt bli en del endringer når arbeidet med § 10-6 kommer videre.
 * Etter at vi har innført v2-modellen så er det kanskje så enkelt å gjøre nye endringer
 * i fremtiden, at vi kan se an hvilke endringer vi vil gjøre her?
 */
enum class BrukersituasjonVilkår {
    // Samme som før
    PRAKTISKE_PROBLEMER_I_DAGLIGLIVET_V1,
    VESENTLIG_OG_VARIG_NEDSATT_FUNKSJONSEVNE_V1,
    KAN_IKKE_LØSES_MED_ENKLERE_HJELPEMIDLER_V1,
    I_STAND_TIL_Å_BRUKE_HJELPEMIDLENE_V1,

    // Overført fra Bruker: val erInformertOmRettigheter: Boolean
    @Deprecated("Ble brukt midlertidig under Covid-19")
    INFORMERT_OM_RETTIGHETER_IFBM_FRITAK_FRA_FULLMAKT_PGA_COVID19,
}

enum class Oppfølgingsansvarlig {
    HJELPEMIDDELFORMIDLER,
    ANNEN_OPPFØLGINGSANSVARLIG,
}

enum class Utleveringsmåte {
    FOLKEREGISTRERT_ADRESSE,
    ANNEN_BRUKSADRESSE,
    HJELPEMIDDELSENTRALEN,

    @Deprecated("Brukes ikke i digital behovsmelding lenger. Sjekker nå automatisk om alle hjm er markert som utlevert.")
    ALLEREDE_UTLEVERT_AV_NAV,
}

enum class Kontaktperson {
    HJELPEMIDDELBRUKER,
    HJELPEMIDDELFORMIDLER,
    ANNEN_KONTAKTPERSON,
}
