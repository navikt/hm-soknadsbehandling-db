package no.nav.hjelpemidler.soknad.db.rolle

import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingType

class RolleService(private val client: RolleClient) {
    suspend fun hentTillatteBehovsmeldingstyper(userToken: String): Set<BehovsmeldingType> = client.hentRoller(userToken).tillatelser.behovsmeldingstyper
}
