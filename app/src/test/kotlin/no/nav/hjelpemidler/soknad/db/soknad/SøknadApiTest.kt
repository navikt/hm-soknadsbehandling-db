package no.nav.hjelpemidler.soknad.db.soknad

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.hjelpemidler.behovsmeldingsmodell.BehovsmeldingStatus
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.HotsakSak
import no.nav.hjelpemidler.behovsmeldingsmodell.sak.InfotrygdSak
import no.nav.hjelpemidler.soknad.db.test.testApplication
import java.util.UUID
import kotlin.test.Test

class SøknadApiTest {
    @Test
    fun `Skal lagre digital behovsmelding`() = testApplication {
        val grunnlag = lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital())
        val søknadId = grunnlag.søknadId
        finnSøknad(søknadId).shouldNotBeNull {
            this.søknadId shouldBe søknadId
            this.status shouldBe grunnlag.status
        }
        finnBehovsmelding(søknadId).shouldNotBeNull {
            this.id shouldBe søknadId
        }
        finnSak<HotsakSak>(søknadId).shouldBeNull()
    }

    @Test
    fun `Skal lagre papirsøknad`() = testApplication {
        val grunnlag = lagreBehovsmelding(lagBehovsmeldingsgrunnlagPapir())
        val sakstilknytning = grunnlag.sakstilknytning.shouldNotBeNull()
        val søknadId = grunnlag.søknadId
        finnSøknad(søknadId).shouldNotBeNull {
            this.søknadId shouldBe søknadId
            this.status shouldBe grunnlag.status
        }
        finnSak<InfotrygdSak>(søknadId).shouldNotBeNull {
            this.sakId shouldBe sakstilknytning.sakId
            this.fnrBruker shouldBe sakstilknytning.fnrBruker
        }
    }

    @Test
    fun `Skal oppdatere søknadsstatus`() = testApplication {
        val status1 = BehovsmeldingStatus.VENTER_GODKJENNING
        val grunnlag = lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital(status = status1))
        val søknadId = grunnlag.søknadId
        finnSøknad(søknadId).shouldNotBeNull {
            this.søknadId shouldBe søknadId
            this.status shouldBe status1
        }
        val status2 = BehovsmeldingStatus.GODKJENT shouldNotBe status1
        oppdaterStatus(søknadId, status2)
        finnSøknad(søknadId).shouldNotBeNull {
            this.søknadId shouldBe søknadId
            this.status shouldBe status2
        }
    }

    @Test
    fun `Skal slette utløpt søknad gjennom statusendring`() = testApplication {
        val status1 = BehovsmeldingStatus.VENTER_GODKJENNING
        val grunnlag = lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital(status = status1))
        val søknadId = grunnlag.søknadId
        finnSøknad(søknadId).shouldNotBeNull {
            this.søknadId shouldBe søknadId
            this.status shouldBe status1
        }
        val status2 = BehovsmeldingStatus.UTLØPT shouldNotBe status1
        oppdaterStatus(søknadId, status2)
        finnSøknad(søknadId).shouldNotBeNull {
            this.søknadId shouldBe søknadId
            this.status shouldBe status2
        }
    }

    @Test
    fun `Skal slette søknad gjennom statusendring`() = testApplication {
        val status1 = BehovsmeldingStatus.VENTER_GODKJENNING
        val grunnlag = lagreBehovsmelding(lagBehovsmeldingsgrunnlagDigital(status = status1))
        val søknadId = grunnlag.søknadId
        finnSøknad(søknadId).shouldNotBeNull {
            this.søknadId shouldBe søknadId
            this.status shouldBe status1
        }
        val status2 = BehovsmeldingStatus.SLETTET shouldNotBe status1
        oppdaterStatus(søknadId, status2)
        finnSøknad(søknadId).shouldNotBeNull {
            this.søknadId shouldBe søknadId
            this.status shouldBe status2
        }
    }

    @Test
    fun `Skal ikke lagre papirsøknad hvis søknad er lagret allerede med samme fnr og journalpostId`() = testApplication {
        val grunnlag = lagBehovsmeldingsgrunnlagPapir()
        lagreBehovsmelding(grunnlag, 1)
        lagreBehovsmelding(grunnlag, 0)
    }

    @Test
    fun `Skal hente søknad`() = testApplication {
        val søknadId = lagreBehovsmelding().søknadId
        finnSøknad(søknadId).shouldNotBeNull {
            this.søknadId shouldBe søknadId
        }
    }

    @Test
    fun `Skal hente søknad som ikke finnes`() = testApplication {
        val søknadId = UUID.randomUUID()
        val søknad = finnSøknad(søknadId)
        søknad shouldBe null
    }

    @Test
    fun `Skal oppdatere journalpostId`() = testApplication {
        val søknadId = lagreBehovsmelding().søknadId
        finnSøknad(søknadId).shouldNotBeNull {
            journalpostId.shouldBeNull()
        }
        val journalpostId = "102030"
        oppdaterJournalpostId(søknadId, journalpostId)
        finnSøknad(søknadId).shouldNotBeNull {
            this.journalpostId shouldBe journalpostId
        }
    }

    @Test
    fun `Skal oppdatere oppgaveId`() = testApplication {
        val søknadId = lagreBehovsmelding().søknadId
        finnSøknad(søknadId).shouldNotBeNull {
            oppgaveId.shouldBeNull()
        }
        val oppgaveId = "302010"
        oppdaterOppgaveId(søknadId, oppgaveId)
        finnSøknad(søknadId).shouldNotBeNull {
            this.oppgaveId shouldBe oppgaveId
        }
    }
}
