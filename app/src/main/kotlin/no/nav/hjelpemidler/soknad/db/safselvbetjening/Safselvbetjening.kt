package no.nav.hjelpemidler.soknad.db.safselvbetjening

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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

    suspend fun hentDokumenter(fnrBruker: String, forFagsakId: String?, onBehalfOfToken: String): List<Journalpost> {
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
                    setBody(req)
                }.body<GraphqlResult>()

                if (res.errors != null) {
                    logg.error { "Error message from safselvbetjening: ${res.errors.toPrettyString()}" }
                    return@withContext emptyList()
                }

                var jps = res.data
                    ?.dokumentoversiktSelvbetjening
                    ?.tema
                    ?.firstOrNull()
                    ?.journalposter
                    ?: emptyList()

                // Sorter resultater
                jps = jps.sortedBy {
                    runCatching {
                        it.relevanteDatoer
                            .find { dat -> dat.datotype == "DATO_OPPRETTET" }
                            ?.dato
                            ?.let { dt -> LocalDateTime.parse(dt) }
                    }.getOrNull()
                }

                // Filtrer ut for fagsaker
                if (forFagsakId != null) {
                    jps = jps.filter { listOf("IT01", "HJELPEMIDLER").contains(it.sak?.fagsaksystem) && it.sak?.fagsakId == forFagsakId }
                }

                jps
            }
        } catch (e: Exception) {
            logg.error(e) { "Henting av dokumenter fra safselvbetjening feilet" }
            throw e
        }
    }
}
