package no.nav.hjelpemidler.soknad.db.rolle

class RolleService(
    private val client: RolleClient,
) {
    suspend fun hentRolle(token: String): InnsenderRolle? {
        val rolleResultat = client.hentRolle(token)

        return if (rolleResultat.formidlerRolle.harFormidlerRolle) {
            InnsenderRolle.FORMIDLER
        } else if (rolleResultat.bestillerRolle?.harBestillerRolle == true) {
            InnsenderRolle.BESTILLER
        } else {
            null
        }
    }
}
