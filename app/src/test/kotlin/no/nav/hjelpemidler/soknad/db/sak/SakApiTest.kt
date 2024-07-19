package no.nav.hjelpemidler.soknad.db.sak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.plugins.resources.get
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.soknad.db.test.expect
import no.nav.hjelpemidler.soknad.db.test.testApplication
import kotlin.test.Test

class SakApiTest {
    @Test
    fun `Skal hente sak med sakId`() = testApplication {
        val søknad = lagreSøknad()
        val sakId = HotsakSakId("2010")
        lagreSakstilknytning(
            søknad.søknadId,
            Sakstilknytning.Hotsak(
                sakId = sakId,
            ),
        )
        client.get(Saker.SakId(sakId.value)).expect<Fagsak>(HttpStatusCode.OK) { fagsak ->
            fagsak.shouldBeInstanceOf<HotsakSak> { hotsakSak ->
                hotsakSak.sakId shouldBe sakId
            }
        }
    }
}
