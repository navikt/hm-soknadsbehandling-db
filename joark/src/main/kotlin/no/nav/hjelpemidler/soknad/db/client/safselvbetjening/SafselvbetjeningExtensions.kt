package no.nav.hjelpemidler.soknad.db.client.safselvbetjening

import no.nav.hjelpemidler.domain.kodeverk.Fagsaksystem
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.enums.Datotype
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.hentdokumentoversiktselvbetjening.Journalpost
import java.time.OffsetDateTime

val Journalpost.datoOpprettet: OffsetDateTime?
    get() = relevanteDatoer.singleOrNull { it?.datotype == Datotype.DATO_OPPRETTET }?.dato

val Journalpost.isRelevantFagsak: Boolean
    get() = sak?.fagsaksystem in relevanteFagsaksystemer

private val relevanteFagsaksystemer = setOf(
    Fagsaksystem.IT01.name,
    Fagsaksystem.HJELPEMIDLER.name,
)
