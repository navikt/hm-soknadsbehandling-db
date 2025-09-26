package no.nav.hjelpemidler.soknad.db.safselvbetjening

import io.ktor.resources.Resource

@Resource("/bruker")
class Bruker {
    @Resource("/dokumenter")
    class Dokumenter(val parent: Bruker = Bruker()) {
        @Resource("/{fagsakId}")
        class ForSak(val fagsakId: String, val parent: Dokumenter = Dokumenter())
    }
}
