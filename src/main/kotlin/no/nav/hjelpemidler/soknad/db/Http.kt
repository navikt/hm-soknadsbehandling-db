package no.nav.hjelpemidler.soknad.db

import io.ktor.client.HttpClient
import no.nav.hjelpemidler.http.createHttpClient

fun httpClient(): HttpClient = createHttpClient()
