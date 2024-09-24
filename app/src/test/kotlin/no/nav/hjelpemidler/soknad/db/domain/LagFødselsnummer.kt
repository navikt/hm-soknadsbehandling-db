package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.domain.person.Fødselsnummer
import java.time.LocalDate
import kotlin.random.Random

/**
 * NB! Lager ikke gyldige fødselsnumre. Kun formatet er riktig.
 */
fun lagFødselsnummer(): String =
    Fødselsnummer(LocalDate.ofEpochDay(Random.nextLong(-14610, LocalDate.now().toEpochDay()))).toString()
