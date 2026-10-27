package no.nav.hjelpemidler.soknad.db.sak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.plugins.resources.get
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.Fagsak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSak
import no.nav.hjelpemidler.server.ktor.security.test.bearerAuth
import no.nav.hjelpemidler.soknad.db.soknad.lagSakstilknytningHotsak
import no.nav.hjelpemidler.soknad.db.test.expect
import no.nav.hjelpemidler.soknad.db.test.testApplication
import kotlin.test.Test

class SakApiTest {
    @Test
    fun `Skal hente sak med sakId`() = testApplication {
        val grunnlag = lagreBehovsmelding()
        val sakstilknytning = lagSakstilknytningHotsak()
        lagreSakstilknytning(grunnlag.søknadId, sakstilknytning)
        client
            .get(Saker.SakId(sakstilknytning.sakId.value)) {
                bearerAuth(systembruker)
            }
            .expect<Fagsak>(HttpStatusCode.OK) { fagsak ->
                fagsak.shouldBeInstanceOf<HotsakSak> { hotsakSak ->
                    hotsakSak.sakId shouldBe sakstilknytning.sakId
                }
            }
    }
}
