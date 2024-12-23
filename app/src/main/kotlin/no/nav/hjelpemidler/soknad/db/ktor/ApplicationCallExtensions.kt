package no.nav.hjelpemidler.soknad.db.ktor

import io.ktor.server.application.ApplicationCall
import java.util.UUID

val ApplicationCall.søknadId: UUID get() = parameters["soknadId"].let(UUID::fromString)
