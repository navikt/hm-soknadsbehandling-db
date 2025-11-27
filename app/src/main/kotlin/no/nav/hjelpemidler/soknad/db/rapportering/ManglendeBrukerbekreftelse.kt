package no.nav.hjelpemidler.soknad.db.rapportering

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.behovsmeldingsmodell.v2.Innsenderbehovsmelding
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.soknad.db.rapportering.epost.ContentType
import no.nav.hjelpemidler.soknad.db.rapportering.epost.EPOST_DIGIHOT
import no.nav.hjelpemidler.soknad.db.rapportering.epost.EpostClient
import no.nav.hjelpemidler.soknad.db.store.BrukerbekreftelseVarselEntity
import no.nav.hjelpemidler.soknad.db.store.Transaction
import no.nav.hjelpemidler.time.toInstant
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

private const val ANTALL_DAGER_FØR_FØRSTE_VARSLING = 3
private const val ANTALL_DAGER_FØR_FORNYET_VARSLING = 7

class ManglendeBrukerbekreftelse(
    private val transaction: Transaction,
    private val epost: EpostClient,
    private val clock: Clock,
) {

    suspend fun rapporter() {

        sendTestepost()

        val eposterSomHarBlittVarsletIDag = transaction {
            brukerbekreftelseVarselStore.hentVarslerForIDag()
        }.toMutableSet()

        val behovsmeldingerSomAvventerBrukerbekreftelse: List<Innsenderbehovsmelding> =
            transaction {
                søknadStore.hentBehovsmeldingerTilGodkjenningEldreEnn(ANTALL_DAGER_FØR_FØRSTE_VARSLING)
            }.map { it.behovsmelding }
        log.info { "Fant ${behovsmeldingerSomAvventerBrukerbekreftelse.size} behovsmeldinger til vurdering for varsling." }

        for (behovsmelding in behovsmeldingerSomAvventerBrukerbekreftelse) {
            log.info { "Vurderer varsling for behovsmelding ${behovsmelding.id}" }
            if (!erModenForVarsling(behovsmelding)) {
                log.info { "Behovsmelding ${behovsmelding.id} er ikke moden for varsling. Hopper over." }
                continue
            }

            val formidlersEpost = behovsmelding.levering.hjelpemiddelformidler.epost
            val eksisterendeVarsel = eposterSomHarBlittVarsletIDag.find { it.formidlersEpost == formidlersEpost }
            if (eksisterendeVarsel != null) {
                log.info { "Behovsmelding ${behovsmelding.id} er moden for varsling, men formidler har allerede blitt varslet i dag. Markerer behovsmelding som varslet." }
                transaction {
                    brukerbekreftelseVarselStore.lagreVarsletBehovsmelding(eksisterendeVarsel.id, behovsmelding.id)
                }
                continue
            }

            log.info { "Behovsmelding ${behovsmelding.id} er moden for varsling og formidler har ikke allerede mottatt varsel i dag. Sender ut varsel." }
            transaction(returnGeneratedKeys = true) {
                val varselId = brukerbekreftelseVarselStore.lagreEpostvarsling(formidlersEpost)
                eposterSomHarBlittVarsletIDag.add(BrukerbekreftelseVarselEntity(varselId, formidlersEpost))
                brukerbekreftelseVarselStore.lagreVarsletBehovsmelding(varselId, behovsmelding.id)
                sendEpostVarsel(formidlersEpost)
            }
        }
    }

    private suspend fun erModenForVarsling(behovsmelding: Innsenderbehovsmelding): Boolean {
        val forrigeVarsling = transaction { brukerbekreftelseVarselStore.hentSisteVarsling(behovsmelding.id) }
        val innsendt = behovsmelding.innsendingstidspunkt ?: behovsmelding.innsendingsdato.toInstant()
        val nå = Instant.now(clock)

        if (forrigeVarsling == null) {
            // Moden for førstegangsvarsling?
            return antallDagerMellom(innsendt, nå) >= ANTALL_DAGER_FØR_FØRSTE_VARSLING
        }

        // Moden for fornyet varsling?
        return antallDagerMellom(forrigeVarsling, nå) >= ANTALL_DAGER_FØR_FORNYET_VARSLING
    }

    private fun antallDagerMellom(a: Instant, b: Instant): Long {
        return ChronoUnit.DAYS.between(a, b)
    }

    private suspend fun sendEpostVarsel(formidlersEpost: String) {
        epost.send(
            avsender = EPOST_DIGIHOT,
            mottaker = formidlersEpost,
            tittel = TITTEL_VARSEL_BRUKERBEKREFTELSE,
            innholdstype = ContentType.TEXT,
            innhold = """
                Hei!
                
                Dette er et automatisk generert varsel om at du har en eller flere behovsmeldinger som ikke har blitt signert av innbygger.
                
                Vi sender deg dette varselet slik at du kan vurdere om det er behov for oppfølging av innbygger i disse sakene.
                
                Du kan se hvilke saker det gjelder ved å logge deg på digital behovsmelding og gå til "Dine innsendte saker". Der vil saker som venter på digital signatur ligge øverst.
                
                Du kan svare oss tilbake på denne eposten dersom noe er uklart.
                
                Vennlig hilsen
                DigiHoT, Nav
            """.trimIndent(),
            lagreIUtboks = true, // TODO skru av etter verifisering
        )
    }

    private suspend fun sendTestepost() {
        epost.send(
            avsender = EPOST_DIGIHOT,
            mottaker = "ole.steinar.lillestol.skrede@nav.no",
            tittel = TITTEL_VARSEL_BRUKERBEKREFTELSE,
            innholdstype = ContentType.TEXT,
            innhold = """
                Hei!
                
                Dette er et automatisk generert varsel om at du har en eller flere behovsmeldinger som ikke har blitt signert av innbygger.
                
                Vi sender deg dette varselet slik at du kan vurdere om det er behov for oppfølging av innbygger i disse sakene.
                
                Du kan se hvilke saker det gjelder ved å logge deg på digital behovsmelding og gå til "Dine innsendte saker". Der vil saker som venter på digital signatur ligge øverst.
                
                Du kan svare oss tilbake på denne eposten dersom noe er uklart.
                
                Vennlig hilsen
                DigiHoT, Nav
            """.trimIndent(),
            lagreIUtboks = true, // TODO skru av etter verifisering
        )
    }

    fun nesteKjøring(): LocalDateTime = LocalDateTime.now(clock).plusMinutes(10) // LocalDateTime.now(clock).plusDays(1).withHour(1).withMinute(0)
}

const val TITTEL_VARSEL_BRUKERBEKREFTELSE = "Hjelpemiddelsaker som venter på signatur fra innbygger"
