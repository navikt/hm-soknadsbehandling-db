package no.nav.hjelpemidler.soknad.db.rolle

data class RolleResultat(
    val formidlerRolle: FormidlerRolle,
    val bestillerRolle: BestillerRolle? = null,
)

data class BestillerRolle(
    val harBestillerRolle: Boolean,
    val erPilotkommune: Boolean = false,
    val erKommunaltAnsatt: Boolean,
//    val godkjenteKommunaleOrganisasjoner: List<Organisasjon>,
    val harAllowlistTilgang: Boolean = false,
    val harGodkjentNæringskode: Boolean = false,
    val harBestillerKurs: Boolean,
    val erGodkjentAvLeder: Boolean,
    val organisasjoner: List<Organisasjon>,
    val feil: List<BestillerRolleFeil>,
)

data class FormidlerRolle(
    val harFormidlerRolle: Boolean,
    val erPilotkommune: Boolean = false,
    val harAltinnRettighet: Boolean = false,
//    val organisasjonerMedAltinnTilgang: List<Organisasjon>,
    val harAllowlistTilgang: Boolean = false,
    val organisasjoner: List<Organisasjon>,
    val organisasjonerManKanBeOmTilgangTil: List<Organisasjon>,
    val godkjenningskurs: List<Godkjenningskurs>,
    val feil: List<FormidlerRolleFeil>,
)

enum class FormidlerRolleFeil {
    GODKJENNINGSKURS,
    ALLOWLIST,
    ALTINN,
}

enum class BestillerRolleFeil {
    GODKJENNINGSKURS,
    AAREG,
    ALLOWLIST,
}

data class Godkjenningskurs(
    val id: Int,
    val title: String,
    val kilde: String,
)

data class Organisasjon(
    val orgnr: String,
    val navn: String,
    val orgform: String = "",
    val overordnetOrgnr: String? = null,
    val næringskoder: List<Næringskode> = emptyList(),
)

data class Næringskode(
    val kode: String,
    val beskrivelse: String = "",
)

enum class InnsenderRolle {
    FORMIDLER,
    BESTILLER,
}
