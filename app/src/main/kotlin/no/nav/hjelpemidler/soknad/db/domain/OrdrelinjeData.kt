package no.nav.hjelpemidler.soknad.db.domain

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class OrdrelinjeData(
    val søknadId: UUID,
    val oebsId: Int,
    val fnrBruker: String,
    val serviceforespørsel: Int?, // Viss det ikkje er ein SF
    val ordrenr: Int,
    val ordrelinje: Int,
    val delordrelinje: Int,
    val artikkelnr: String,
    val antall: Double,
    val enhet: String,
    val produktgruppe: String,
    val produktgruppeNr: String,
    val hjelpemiddeltype: String,
    val data: JsonNode,
)
