package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.BehovsmeldingType
import no.nav.hjelpemidler.behovsmeldingsmodell.v1.Brukerpassbytte
import java.time.LocalDate
import java.util.UUID

// Kan vi ha en minimal Behovsmelding-klasse som er den som legges på Kafka og som dekker behovet til de
// fleste apper (som ikke har behov for hele behovsmeldingen)?
open class Behovsmelding (
    val id: UUID,
    val type: BehovsmeldingType,
    val innsendingsdato: LocalDate,
    val prioritet: Prioritet,

    // TODO Vi trenger kanskje brukersFnr og innsendersFnr her også.
){
    val skjemaversjon: String = "v2" // Greit å ha for å enkelt kunne mappe mellom versjoner i fremtiden?
}



/*

Alternativ som bruker Jackson til å utlede subklassevariant basert på behovsmeldingType

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Brukerpassbytte::class, name = "BRUKERPASSBYTTE"),
    JsonSubTypes.Type(value = FormidlerBehovsmelding::class, name = "SØKNAD"),
    JsonSubTypes.Type(value = FormidlerBehovsmelding::class, name = "BESTILLING"),
    JsonSubTypes.Type(value = FormidlerBehovsmelding::class, name = "BYTTE")
)
interface Behovsmelding {
    val id: UUID
    val type: BehovsmeldingType
    val innsendingsdato: LocalDate
}

*/

/*

Alternativ som tilsvarer eksisterende løsning

data class Behovsmelding(
    val id: UUID,
    val type: BehovsmeldingType,
    val innsendingsdato: LocalDate,
    val brukerpassbytte: Brukerpassbytte?,
    val formidlerBehovsmelding: FormidlerBehovsmelding?,
) {
    init {
        if (type == BehovsmeldingType.BRUKERPASSBYTTE) {
            require(brukerpassbytte != null) {"Property brukerpassbytte må være satt for $type"}
        } else {
            require(formidlerBehovsmelding != null) {"Property formidlerBehovsmelding må være satt for $type"}
        }
    }
}
 */