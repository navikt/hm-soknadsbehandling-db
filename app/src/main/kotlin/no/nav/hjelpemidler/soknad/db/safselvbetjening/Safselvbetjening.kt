package no.nav.hjelpemidler.soknad.db.safselvbetjening

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.RoutingCall
import io.ktor.util.filter
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.hjelpemidler.http.correlationId
import no.nav.hjelpemidler.http.createHttpClient
import no.nav.hjelpemidler.soknad.db.Configuration
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder
import java.time.LocalDateTime

private val logg = KotlinLogging.logger { }

class Safselvbetjening(
    private val url: String = Configuration.SAFSELVBETJENING_URL,
    private val audience: String = Configuration.SAFSELVBETJENING_AUDIENCE,
) {
    private val tokendingsService = TokendingsServiceBuilder.buildTokendingsService()
    private val client: HttpClient = createHttpClient(Apache.create())

    suspend fun hentDokumenter(
        onBehalfOfToken: String,
        fnrBruker: String,
        forFagsakId: String? = null,
    ): List<Journalpost> {
        val req = GraphqlRequest(
            query = """
                query (${'$'}ident: String!) {
                    dokumentoversiktSelvbetjening(ident: ${'$'}ident, tema: [HJE]) {
                        tema {
                            journalposter {
                                journalpostId
                                tittel
                                journalposttype
                                journalstatus
                                relevanteDatoer {
                                    dato
                                    datotype
                                }
                                kanal
                                sak {
                                    fagsaksystem
                                    fagsakId
                                }
                                dokumenter {
                                    tittel
                                    dokumentInfoId
                                    brevkode
                                    dokumentvarianter {
                                        variantformat
                                        brukerHarTilgang
                                        code
                                    }
                                }
                            }
                        }
                    }
                }                
            """.trimIndent(),
            variables = mapOf(
                "ident" to fnrBruker,
            ),
        )
        return try {
            withContext(Dispatchers.IO) {
                // Token exchange og graphql request
                val exchangedToken = tokendingsService.exchangeToken(onBehalfOfToken, audience)
                val res = client.post("$url/graphql") {
                    bearerAuth(exchangedToken)
                    correlationId()
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }

                if (!res.status.isSuccess()) {
                    val textBody = runCatching { res.bodyAsText() }.getOrNull()
                    logg.error { "Failed to run graphql query against safselvbetjening: $textBody" }
                    return@withContext emptyList()
                }

                val body = res.body<GraphqlResult>()
                if (body.errors != null) {
                    logg.error { "Error message from safselvbetjening: ${body.errors.toPrettyString()}" }
                    return@withContext emptyList()
                }

                var jps = body.data
                    ?.dokumentoversiktSelvbetjening
                    ?.tema
                    ?.firstOrNull()
                    ?.journalposter
                    ?: emptyList()

                // Filtrer ut for fagsaker
                if (forFagsakId != null) {
                    jps = jps.filter {
                        listOf(
                            "IT01",
                            "HJELPEMIDLER",
                        ).contains(it.sak?.fagsaksystem) &&
                            it.sak?.fagsakId == forFagsakId
                    }
                }

                // Sorter resultater
                jps = jps.sortedByDescending {
                    runCatching {
                        it.relevanteDatoer
                            .find { dat -> dat.datotype == "DATO_OPPRETTET" }
                            ?.dato
                            ?.let { dt -> LocalDateTime.parse(dt) }
                    }.getOrNull()
                }

                jps
            }
        } catch (e: Exception) {
            logg.error(e) { "Henting av dokumenter fra safselvbetjening feilet" }
            throw e
        }
    }

    suspend fun hentPdfDokumentProxy(
        onBehalfOfToken: String,
        proxyTo: RoutingCall,
        journalpostId: String,
        dokumentId: String,
        dokumentvariant: String,
    ) {
        withContext(Dispatchers.IO) {
            // Token exchange og graphql request
            val exchangedToken = tokendingsService.exchangeToken(onBehalfOfToken, audience)
            val upstreamResponse = client.get("$url/rest/hentdokument/$journalpostId/$dokumentId/$dokumentvariant") {
                expectSuccess = false
                bearerAuth(exchangedToken)
                correlationId()
                // Proxy headers / method
                method = proxyTo.request.httpMethod
                headers {
                    appendAll(
                        proxyTo.request.headers.filter { key, _ ->
                            !key.equals(
                                HttpHeaders.ContentType,
                                ignoreCase = true,
                            ) &&
                                !key.equals(
                                    HttpHeaders.ContentLength,
                                    ignoreCase = true,
                                ) &&
                                !key.equals(HttpHeaders.Host, ignoreCase = true)
                        },
                    )
                }
            }
            // Proxy response
            val responseBodyChannel: ByteReadChannel = upstreamResponse.bodyAsChannel()
            proxyTo.response.headers.let { headers ->
                upstreamResponse.headers.filter { key, _ ->
                    !key.equals(
                        HttpHeaders.ContentType,
                        ignoreCase = true,
                    ) &&
                        !key.equals(HttpHeaders.ContentLength, ignoreCase = true)
                }.forEach { key, value ->
                    headers.append(key, value.first())
                }
                // Support use of api in iframes
                headers.append("X-Frame-Options", "SAMEORIGIN")
            }
            proxyTo.respondBytesWriter(
                contentType = upstreamResponse.contentType(),
                status = upstreamResponse.status,
                contentLength = upstreamResponse.contentLength(),
            ) {
                // Pipe the body
                responseBodyChannel.copyTo(this)
            }
        }
    }
}
