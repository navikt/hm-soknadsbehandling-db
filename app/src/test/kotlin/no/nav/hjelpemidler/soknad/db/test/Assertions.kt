package no.nav.hjelpemidler.soknad.db.test

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import no.nav.hjelpemidler.soknad.db.exception.Feilmelding

suspend inline fun <reified T> HttpResponse.expect(
    status: HttpStatusCode,
    matcher: (T) -> Unit = {},
): HttpResponse {
    this shouldHaveStatus status
    this.body<T>().also { matcher(it) }
    return this
}

suspend inline fun <reified T> HttpResponse.expect(
    status: HttpStatusCode,
    body: T,
): HttpResponse = expect<T>(status) { it shouldBe body }

suspend fun HttpResponse.feilmelding(
    status: HttpStatusCode,
    matcher: (Feilmelding) -> Unit = {},
): HttpResponse = expect<Feilmelding>(status) { matcher(it) }
