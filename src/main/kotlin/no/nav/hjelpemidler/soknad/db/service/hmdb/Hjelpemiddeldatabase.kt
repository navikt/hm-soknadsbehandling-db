package no.nav.hjelpemidler.soknad.db.service.hmdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.nav.hjelpemidler.soknad.db.Configuration
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

private val mapperJson = jacksonObjectMapper().registerModule(JavaTimeModule())

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

@ExperimentalTime
class Hjelpemiddeldatabase {
    companion object {
        private var database: List<Hjelpemiddel>? = null

        fun loadDatabase() {
            if (database == null) {
                try {
                    var statusCode = 0
                    val apiURL = "${Configuration.application.grunndataApiURL}/dineHjelpemidlerProdukter"
                    val elapsed: kotlin.time.Duration = measureTime {
                        val client = HttpClient.newBuilder().build()
                        val request = HttpRequest.newBuilder()
                            .uri(URI.create(apiURL))
                            .timeout(Duration.ofSeconds(30))
                            .header("Accepts", "application/json")
                            .GET()
                            .build()
                        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
                        statusCode = response.statusCode()
                        if (statusCode == 200) {
                            database = mapperJson.readValue(response.body().toString())
                        }
                    }
                    if (statusCode == 200) {
                        logg.info("Grunndata-api dataset download successful: timeElapsed=${elapsed.inMilliseconds}ms url=$apiURL")
                        if (database?.size == 0) {
                            logg.info("The grunndata-api dataset we downloaded was empty, falling back to cached version!")
                            database = mapperJson.readValue(javaClass.getResource("/dineHjelpemidlerProdukter.json").readText())
                        }
                    } else {
                        logg.error("Unable to download the dataset from grunndata-api (using cached): statusCode=$statusCode url=$apiURL")
                        database = mapperJson.readValue(javaClass.getResource("/dineHjelpemidlerProdukter.json").readText())
                    }
                } catch (e: Exception) {
                    logg.error("Downloading dataset from grunndata-api threw an exception: $e. Loading dataset from local cache.")
                    e.printStackTrace()
                    database = mapperJson.readValue(javaClass.getResource("/dineHjelpemidlerProdukter.json").readText())
                }
                logg.info("Grunndata-api dataset loaded with ${database?.size} items")
            }
        }

        fun findByHmsNr(hmsNr: Int): Hjelpemiddel? {
            if (database == null) return null
            database!!.forEach {
                if (it.hmsnr == hmsNr) {
                    return it
                }
            }
            return null
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Hjelpemiddel(
    val hmsnr: Int?,
    val artname: String?,
    val pshortdesc: String?,
    val isotitle: String?,
    val blobfileURL: String?,
    val prodid: Int?,
    val artid: Int?,
)
