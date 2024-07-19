package no.nav.hjelpemidler.soknad.db.sak

import io.ktor.resources.Resource

@Resource("/sak")
class Saker {
    @Resource("/{sakId}")
    class SakId(val sakId: String, val parent: Saker = Saker())
}
