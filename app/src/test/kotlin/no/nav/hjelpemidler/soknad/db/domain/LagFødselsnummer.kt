package no.nav.hjelpemidler.soknad.db.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * NB! Lager ikke gyldige fødselsnumre. Kun formatet er riktig.
 */
fun lagFødselsnummer(): String = LocalDate
    .ofEpochDay(Random.nextLong(-14610, LocalDate.now().toEpochDay()))
    .format(DateTimeFormatter.ofPattern("ddMMyy")) + Random
    .nextLong(0, 99999)
    .toString()
    .padStart(5, '0')
