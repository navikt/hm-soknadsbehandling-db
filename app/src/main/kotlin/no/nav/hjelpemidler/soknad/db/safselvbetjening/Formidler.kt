package no.nav.hjelpemidler.soknad.db.safselvbetjening

import io.ktor.resources.Resource

@Resource("/formidler")
class Formidler {
    @Resource("/dokumenter")
    class Dokumenter(val parent: Formidler = Formidler()) {
        @Resource("/{fagsakId}")
        class ForSak(val fagsakId: String, val parent: Dokumenter = Dokumenter())
    }
}
