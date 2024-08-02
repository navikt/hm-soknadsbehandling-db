package no.nav.hjelpemidler.behovsmeldingsmodell.ordre

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.hjelpemidler.behovsmeldingsmodell.TilknyttetSøknad
import java.util.UUID

data class Ordrelinje(
    override val søknadId: UUID,
    val oebsId: Int,
    val fnrBruker: String,
    /**
     * Nullable fordi vi ikke alltid har en SF?
     */
    val serviceforespørsel: Int?,
    val ordrenr: Int,
    val ordrelinje: Int,
    val delordrelinje: Int,
    val artikkelnr: String,
    val antall: Double,
    /**
     * e.g.: "PAR" | "STK" | "" | null
     */
    val enhet: String,
    val produktgruppe: String,
    @JsonAlias("produktgruppeNr")
    val produktgruppenr: String,
    /**
     * e.g.: "Hjelpemiddel" | "Del" | "Individstyrt hjelpemiddel"
     */
    val hjelpemiddeltype: String,
    val data: Map<String, Any?>, // JsonNode
) : TilknyttetSøknad {
    val forHjelpemiddel: Boolean @JsonIgnore get() = hjelpemiddeltype == "Hjelpemiddel"
    val forDel: Boolean @JsonIgnore get() = hjelpemiddeltype == "Del"
    val forIndividstyrtHjelpemiddel: Boolean @JsonIgnore get() = hjelpemiddeltype == "Individstyrt hjelpemiddel"
}
