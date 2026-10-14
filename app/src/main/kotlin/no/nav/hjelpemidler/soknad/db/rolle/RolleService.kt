package no.nav.hjelpemidler.soknad.db.rolle

class RolleService(private val client: RolleClient) {
    suspend fun hentRolle(userToken: String): InnsenderRolle? {
        val rolleResultat = client.hentRolle(userToken)
        return when {
            rolleResultat.formidlerRolle.harFormidlerRolle -> InnsenderRolle.FORMIDLER
            rolleResultat.bestillerRolle?.harBestillerRolle == true -> InnsenderRolle.BESTILLER
            else -> null
        }
    }
}
