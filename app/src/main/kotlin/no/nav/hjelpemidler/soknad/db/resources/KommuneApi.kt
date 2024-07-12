package no.nav.hjelpemidler.soknad.db.resources

import io.ktor.resources.Resource

@Resource("/kommune-api")
class KommuneApi {
    @Resource("/soknader")
    class Søknader(val parent: KommuneApi = KommuneApi())
}
