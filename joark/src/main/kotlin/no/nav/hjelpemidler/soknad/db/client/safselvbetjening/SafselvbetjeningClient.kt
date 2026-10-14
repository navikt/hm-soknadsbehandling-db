package no.nav.hjelpemidler.soknad.db.client.safselvbetjening

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.util.filter
import no.nav.hjelpemidler.domain.person.PersonId
import no.nav.hjelpemidler.http.correlationId
import no.nav.hjelpemidler.http.openid.TokenSetProvider
import no.nav.hjelpemidler.http.openid.onBehalfOf
import no.nav.hjelpemidler.http.openid.openID
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.soknad.db.client.safselvbetjening.hentdokumentoversiktselvbetjening.Journalpost
import no.nav.hjelpemidler.text.toURL
import tools.jackson.databind.json.JsonMapper

private val log = KotlinLogging.logger {}

class SafselvbetjeningClient(
    engine: HttpClientEngine,
    tokenSetProvider: TokenSetProvider,
    private val baseUrl: String,
) {
    private val graphQLClient = GraphQLKtorClient(
        url = "$baseUrl/graphql".toURL(),
        httpClient = HttpClient(engine) {
            openID(tokenSetProvider)
            defaultRequest { correlationId() }
        },
        serializer = GraphQLClientJacksonSerializer(jsonMapper as JsonMapper),
    )

    private val httpClient = HttpClient(engine) {
        expectSuccess = false
        openID(tokenSetProvider)
        defaultRequest { correlationId() }
    }

    suspend fun hentJournalposter(
        userToken: String,
        id: PersonId,
        fagsakId: String? = null,
    ): List<Journalpost> {
        val variables = HentDokumentoversiktSelvbetjening.Variables(ident = id.toString())

        val response = try {
            graphQLClient.execute(HentDokumentoversiktSelvbetjening(variables)) {
                onBehalfOf(userToken)
            }
        } catch (e: Exception) {
            log.error(e) { "Kunne ikke hente journalposter for person" }
            throw e
        }

        val errors = response.errors
        if (!errors.isNullOrEmpty()) {
            log.error { "Kunne ikke hente journalposter for person, errors: ${errors.map { it.message }}" }
            return emptyList()
        }

        val dokumentoversikt = response.data?.dokumentoversiktSelvbetjening ?: return emptyList()
        val sakstema = dokumentoversikt.tema.firstOrNull() ?: return emptyList()
        return sakstema.journalposter
            .filterNotNull()
            .filter {
                fagsakId == null || (it.sak?.fagsakId == fagsakId && it.isRelevantFagsak)
            }
            .sortedByDescending { it.datoOpprettet }
    }

    suspend fun hentDokument(
        userToken: String,
        headers: Headers,
        journalpostId: String,
        dokumentId: String,
        dokumentvariant: String,
    ): HttpResponse {
        val url = "$baseUrl/rest/hentdokument/$journalpostId/$dokumentId/$dokumentvariant"
        return try {
            httpClient.get(url) {
                onBehalfOf(userToken)
                headers {
                    appendAll(
                        headers.filter { key, _ ->
                            !key.equals(HttpHeaders.ContentType, ignoreCase = true) &&
                                !key.equals(HttpHeaders.ContentLength, ignoreCase = true) &&
                                !key.equals(HttpHeaders.Host, ignoreCase = true)
                        },
                    )
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Kunne ikke hente dokument, journalpostId: $journalpostId, dokumentId: $dokumentId, dokumentvariant: $dokumentvariant" }
            throw e
        }
    }
}
