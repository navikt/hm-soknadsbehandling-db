package no.nav.hjelpemidler.behovsmeldingsmodell.v2

import no.nav.hjelpemidler.behovsmeldingsmodell.OppfølgingsansvarligV2
import no.nav.hjelpemidler.behovsmeldingsmodell.UtleveringsmåteV2
import no.nav.hjelpemidler.domain.geografi.Veiadresse
import no.nav.hjelpemidler.domain.person.Personnavn

fun lagLevering(
    oppfølgingsansvarligAnsvarFor: String? = null,
    utleveringsmåte: UtleveringsmåteV2? = null,
    utleveringMerknad: String = "",
): Levering = Levering(
    hjelpemiddelformidler = Levering.Hjelpemiddelformidler(
        navn = navn(),
        arbeidssted = "",
        stilling = "",
        telefon = "",
        adresse = veiadresse(),
        epost = "",
        treffesEnklest = "",
        kommunenavn = "",
    ),
    oppfølgingsansvarlig = OppfølgingsansvarligV2.ANNEN_OPPFØLGINGSANSVARLIG,
    annenOppfølgingsansvarlig = when (oppfølgingsansvarligAnsvarFor) {
        null -> null
        else -> Levering.AnnenOppfølgingsansvarlig(
            navn = navn(),
            arbeidssted = "",
            stilling = "",
            telefon = "",
            ansvarFor = oppfølgingsansvarligAnsvarFor,
        )
    },
    utleveringsmåte = utleveringsmåte,
    annenUtleveringsadresse = veiadresse(),
    utleveringKontaktperson = null,
    annenKontaktperson = null,
    utleveringMerknad = utleveringMerknad,
    hast = null,
)

private fun veiadresse() = Veiadresse("adresse", "1234", "poststed")
private fun navn() = Personnavn(fornavn = "Egon", etternavn = "Olsen")
