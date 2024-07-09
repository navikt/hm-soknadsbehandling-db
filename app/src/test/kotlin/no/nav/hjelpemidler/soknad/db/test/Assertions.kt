package no.nav.hjelpemidler.soknad.db.test

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

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
