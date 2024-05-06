package no.nav.hjelpemidler.soknad.db.db

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.hjelpemidler.soknad.db.domain.BehovsmeldingType
import no.nav.hjelpemidler.soknad.db.domain.SoknadData
import no.nav.hjelpemidler.soknad.db.domain.Status
import no.nav.hjelpemidler.soknad.db.mockSøknad
import no.nav.hjelpemidler.soknad.db.rolle.InnsenderRolle
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class SøknadStoreInnsenderTest {

    @Test
    fun `Hent formidlers søknad`() {
        val soknadsId = UUID.randomUUID()
        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    mockSøknad(soknadsId),
                )
            }
            SøknadStoreInnsenderPostgres(DataSource.instance).apply {
                val formidlersSøknad = this.hentSøknaderForInnsender("12345678910", InnsenderRolle.FORMIDLER)[0]
                assertEquals("fornavn etternavn", formidlersSøknad.navnBruker)
            }
        }
    }

    @Test
    fun `Formidler kan kun hente søknad hen selv har sendt inn`() {
        val soknadsId = UUID.randomUUID()
        val fnrFormidler = "12345678910"
        val fnrAnnenFormidler = "10987654321"
        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    mockSøknad(soknadsId, fnrInnsender = fnrFormidler),
                )
                this.save(
                    mockSøknad(UUID.randomUUID(), fnrInnsender = fnrAnnenFormidler),
                )
            }
            SøknadStoreInnsenderPostgres(DataSource.instance).apply {
                val formidlersSoknader = this.hentSøknaderForInnsender("12345678910", InnsenderRolle.FORMIDLER)
                assertEquals(1, formidlersSoknader.size)

                val formidlersSoknad = this.hentSøknadForInnsender("12345678910", soknadsId, InnsenderRolle.FORMIDLER)
                assertEquals(soknadsId, formidlersSoknad!!.søknadId)
                assertNotNull(formidlersSoknad.søknadsdata)
            }
        }
    }

    @Test
    fun `Navn på bruker fjernes ikke ved sletting`() {
        val soknadId = UUID.randomUUID()

        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    mockSøknad(soknadId, Status.VENTER_GODKJENNING),
                ).also {
                    it shouldBe 1
                }

                this.slettSøknad(soknadId).also {
                    it shouldBe 1
                }

                SøknadStoreInnsenderPostgres(DataSource.instance).apply {
                    val formidlersSøknad = this.hentSøknaderForInnsender("12345678910", InnsenderRolle.FORMIDLER)[0]
                    assertEquals("fornavn etternavn", formidlersSøknad.navnBruker)
                }
            }
        }
    }

    @Test
    fun `Henter ikke søknad som er 4 uker gammel`() {
        val id = UUID.randomUUID()

        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    SoknadData(
                        "id",
                        "fornavn etternavn",
                        "12345678910",
                        id,
                        ObjectMapper().readTree(""" {"key": "value"} """),
                        status = Status.SLETTET,
                        kommunenavn = null,
                        er_digital = true,
                        soknadGjelder = null,
                    ),
                ).also {
                    it shouldBe 1
                }
            }
            DataSource.instance.apply {
                sessionOf(this).run(queryOf("UPDATE V1_SOKNAD SET CREATED = (now() - interval '3 week') WHERE SOKNADS_ID = '$id' ").asExecute)
            }

            SøknadStoreInnsenderPostgres(DataSource.instance).apply {
                this.hentSøknaderForInnsender("12345678910", InnsenderRolle.FORMIDLER, 4).also {
                    it.size shouldBe 1
                }
            }

            DataSource.instance.apply {
                sessionOf(this).run(queryOf("UPDATE V1_SOKNAD SET CREATED = (now() - interval '5 week') WHERE SOKNADS_ID = '$id' ").asExecute)
            }

            SøknadStoreInnsenderPostgres(DataSource.instance).apply {
                this.hentSøknaderForInnsender("1234567891014", InnsenderRolle.FORMIDLER, 4).also {
                    it.size shouldBe 0
                }
            }
        }
    }

    @Test
    fun `Fullmakt for søknad for formidler`() {
        val soknadId = UUID.randomUUID()

        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    mockSøknad(soknadId, Status.GODKJENT_MED_FULLMAKT),
                ).also {
                    it shouldBe 1
                }

                this.oppdaterStatus(soknadId, Status.ENDELIG_JOURNALFØRT).also {
                    it shouldBe 1
                }
            }
            SøknadStoreInnsenderPostgres(DataSource.instance).apply {
                val søknader = this.hentSøknaderForInnsender("12345678910", InnsenderRolle.FORMIDLER)
                assertTrue { søknader[0].fullmakt }
            }
        }
    }

    @Test
    fun `Fullmakt er false hvis bruker skal bekrefte søknaden`() {
        val soknadId = UUID.randomUUID()

        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    mockSøknad(soknadId, Status.VENTER_GODKJENNING),
                ).also {
                    it shouldBe 1
                }

                this.oppdaterStatus(
                    soknadId,
                    Status.GODKJENT,
                ).also {
                    println(it)
                    it shouldBe 1
                }

                this.oppdaterStatus(soknadId, Status.ENDELIG_JOURNALFØRT).also {
                    it shouldBe 1
                }
            }
            SøknadStoreInnsenderPostgres(DataSource.instance).apply {
                val søknader = this.hentSøknaderForInnsender("12345678910", InnsenderRolle.FORMIDLER)
                assertFalse { søknader[0].fullmakt }
            }
        }
    }

    @Test
    fun `Bestiller kan kun hente ut BESTILLINGer`() {
        val idSøknad = UUID.randomUUID()
        val idBestilling = UUID.randomUUID()
        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(mockSøknad(idSøknad, behovsmeldingType = BehovsmeldingType.SØKNAD))
                this.save(mockSøknad(idBestilling, behovsmeldingType = BehovsmeldingType.BESTILLING))
            }

            SøknadStoreInnsenderPostgres(DataSource.instance).apply {
                this.hentSøknaderForInnsender("12345678910", InnsenderRolle.BESTILLER).also {
                    it.size shouldBe 1
                    it.first().søknadId shouldBe idBestilling
                }
            }

            SøknadStoreInnsenderPostgres(DataSource.instance).apply {
                this.hentSøknadForInnsender("12345678910", idBestilling, InnsenderRolle.BESTILLER).also {
                    it!!.søknadId shouldBe idBestilling
                    it.behovsmeldingType shouldBe BehovsmeldingType.BESTILLING
                }
            }
        }
    }

    @Test
    fun `Metrikker er non-blocking`() {
        val soknadId = UUID.randomUUID()
        withMigratedDb {
            SøknadStorePostgres(DataSource.instance).apply {
                this.save(
                    mockSøknad(soknadId, Status.VENTER_GODKJENNING),
                ).also {
                    it shouldBe 1
                }

                this.oppdaterStatus(
                    soknadId,
                    Status.GODKJENT,
                ).also {
                    println(it)
                    it shouldBe 1
                }

                this.oppdaterStatus(soknadId, Status.VEDTAKSRESULTAT_ANNET).also {
                    it shouldBe 1
                }
            }
        }
    }
}
