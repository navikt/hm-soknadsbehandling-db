package no.nav.hjelpemidler.soknad.db.domain

import no.nav.hjelpemidler.domain.person.Fødselsnummer
import no.nav.hjelpemidler.domain.person.år

fun lagFødselsnummer(): Fødselsnummer = Fødselsnummer((0..99).random().år)
