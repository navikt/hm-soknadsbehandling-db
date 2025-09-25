package no.nav.hjelpemidler.soknad.db

import io.ktor.resources.Resource
import no.nav.hjelpemidler.soknad.db.soknad.Søknader

@Resource("/bruker")
class Bruker {
    @Resource("/dokumenter")
    class Dokumenter(val parent: Søknader = Søknader()) {
        @Resource("/{fagsakId}")
        class ForSak(val fagsakId: String, val parent: Dokumenter = Dokumenter())
    }
}
