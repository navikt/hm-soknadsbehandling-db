package no.nav.hjelpemidler.soknad.db

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import no.nav.hjelpemidler.http.createHttpClient

fun httpClient(): HttpClient = createHttpClient(Apache.create())
