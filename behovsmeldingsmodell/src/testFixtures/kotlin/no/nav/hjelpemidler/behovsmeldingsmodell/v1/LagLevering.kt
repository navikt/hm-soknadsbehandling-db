package no.nav.hjelpemidler.behovsmeldingsmodell.v1

import no.nav.hjelpemidler.behovsmeldingsmodell.Kontaktperson
import no.nav.hjelpemidler.behovsmeldingsmodell.Oppfølgingsansvarlig
import no.nav.hjelpemidler.behovsmeldingsmodell.Utleveringsmåte

fun lagLevering(
    oppfølgingsansvarlig: Oppfølgingsansvarlig? = null,
    oppfølgingsansvarligAnsvarFor: String? = null,
    utleveringsmåte: Utleveringsmåte? = null,
    utleveringKontaktperson: Kontaktperson? = null,
    utleveringMerknad: String = "",
): Levering = Levering(
    hjelpemiddelformidlerFornavn = "",
    hjelpemiddelformidlerEtternavn = "",
    hjelpemiddelformidlerArbeidssted = "",
    hjelpemiddelformidlerStilling = "",
    hjelpemiddelformidlerTelefon = "",
    hjelpemiddelformidlerEpost = "",
    hjelpemiddelformidlerPostadresse = "",
    hjelpemiddelformidlerPostnummer = "",
    hjelpemiddelformidlerPoststed = "",
    hjelpemiddelformidlerTreffesEnklest = "",
    oppfølgingsansvarlig = oppfølgingsansvarlig,
    oppfølgingsansvarligFornavn = null,
    oppfølgingsansvarligEtternavn = null,
    oppfølgingsansvarligArbeidssted = null,
    oppfølgingsansvarligStilling = null,
    oppfølgingsansvarligTelefon = null,
    oppfølgingsansvarligAnsvarFor = oppfølgingsansvarligAnsvarFor,
    utleveringsmåte = utleveringsmåte,
    utleveringKontaktperson = utleveringKontaktperson,
    utleveringFornavn = null,
    utleveringEtternavn = null,
    utleveringTelefon = null,
    utleveringPostadresse = null,
    utleveringPostnummer = null,
    utleveringPoststed = null,
    utleveringMerknad = utleveringMerknad,
)
