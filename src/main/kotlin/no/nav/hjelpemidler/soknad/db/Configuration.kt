package no.nav.hjelpemidler.soknad.db

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException

private val localProperties = ConfigurationMap(
    mapOf(
        "application.httpPort" to "8082",
        "application.profile" to "LOCAL",
        "db.host" to "host.docker.internal",
        "db.database" to "soknadsbehandling",
        "db.password" to "postgres",
        "db.port" to "5434",
        "db.username" to "postgres",
        "userclaim" to "sub",

    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "application.httpPort" to "8080",
        "application.profile" to "DEV",
        "userclaim" to "pid",
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "application.httpPort" to "8080",
        "application.profile" to "PROD",
        "userclaim" to "pid",
    )
)

private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-gcp" -> systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-gcp" -> systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> {
        systemProperties() overriding EnvironmentVariables overriding localProperties
    }
}

internal object Configuration {
    val database: Database = Database()
    val application: Application = Application()

    data class Database(
        val host: String = config()[Key("db.host", stringType)],
        val port: String = config()[Key("db.port", stringType)],
        val name: String = config()[Key("db.database", stringType)],
        val user: String? = config().getOrNull(Key("db.username", stringType)),
        val password: String? = config().getOrNull(Key("db.password", stringType))
    )

    data class Application(
        val id: String = config().getOrElse(Key("", stringType), "hm-soknadsbehandling-db-v1"),
        val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
        val httpPort: Int = config()[Key("application.httpPort", intType)],
        val userclaim: String = config()[Key("userclaim", stringType)]
    )
}

enum class Profile {
    LOCAL, DEV, PROD
}

private fun getHostname(): String {
    return try {
        val addr: InetAddress = InetAddress.getLocalHost()
        addr.hostName
    } catch (e: UnknownHostException) {
        "unknown"
    }
}

private fun String.readFile() =
    File(this).readText(Charsets.UTF_8)
