package no.nav.hjelpemidler.soknad.db.rapportering

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.hjelpemidler.http.createHttpClient
import no.nav.hjelpemidler.soknad.db.Configuration
import java.net.InetAddress

interface LeaderElection {
    suspend fun erLeder(): Boolean
}

class NaisLeaderElection(
    private val httpClient: HttpClient = createHttpClient(Apache.create()),
    private val url: String = Configuration.ELECTOR_GET_URL,
) : LeaderElection {

    override suspend fun erLeder(): Boolean {
        val leaderHostname = withContext(Dispatchers.IO) {
            httpClient.get(url).body<LeaderResponse>().name
        }

        val currentHostname = withContext(Dispatchers.IO) {
            InetAddress.getLocalHost()
        }?.hostName ?: return false

        return currentHostname == leaderHostname
    }
}

data class LeaderResponse(
    val name: String,
)
