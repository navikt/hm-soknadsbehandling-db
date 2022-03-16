package no.nav.hjelpemidler.soknad.db

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

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
        "sensu" to "http://localhost:8456/sensu", // hm-soknad-api WireMock endpoint
        "NAIS_APP_NAME" to "hm-soknadsbehandling-db",
        "NAIS_CLUSTER_NAME" to "dev-gcp",
        "NAIS_NAMESPACE" to "teamdigihot",
        "INFLUX_HOST" to "http://localhost",
        "INFLUX_PORT" to "1234",
        "INFLUX_DATABASE_NAME" to "defaultdb",
        "INFLUX_USER" to "user",
        "INFLUX_PASSWORD" to "password",

        "GRUNNDATA_API_URL" to "https://hm-grunndata-api.dev.intern.nav.no",
        "BIGQUERY_DATASET_ID" to "hm_soknadsbehandling_v1_dataset_local",
        "GCP_TEAM_PROJECT_ID" to "teamdigihot",
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "DEV",
        "userclaim" to "pid",
        "sensu" to "https://digihot-proxy.dev-fss-pub.nais.io/sensu",

        "GRUNNDATA_API_URL" to "http://hm-grunndata-api",
        "BIGQUERY_DATASET_ID" to "hm_soknadsbehandling_v1_dataset_dev",
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "PROD",
        "userclaim" to "pid",
        "sensu" to "https://digihot-proxy.prod-fss-pub.nais.io/sensu",

        "GRUNNDATA_API_URL" to "http://hm-grunndata-api",
        "BIGQUERY_DATASET_ID" to "hm_soknadsbehandling_v1_dataset_prod",
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
    val config = config()
    val database: Database = Database()
    val application: Application = Application()
    val bigQuery: BigQuery = BigQuery()

    data class Database(
        val host: String = config[Key("db.host", stringType)],
        val port: String = config[Key("db.port", stringType)],
        val name: String = config[Key("db.database", stringType)],
        val user: String? = config.getOrNull(Key("db.username", stringType)),
        val password: String? = config.getOrNull(Key("db.password", stringType))
    )

    data class Application(
        val id: String = config.getOrElse(Key("", stringType), "hm-soknadsbehandling-db-v1"),
        val profile: Profile = config[Key("application.profile", stringType)].let { Profile.valueOf(it) },
        val userclaim: String = config[Key("userclaim", stringType)],
        val sensu: String? = config[Key("sensu", stringType)],
        val NAIS_APP_NAME: String? = config[Key("NAIS_APP_NAME", stringType)],
        val NAIS_CLUSTER_NAME: String? = config[Key("NAIS_CLUSTER_NAME", stringType)],
        val NAIS_NAMESPACE: String? = config[Key("NAIS_NAMESPACE", stringType)],
        val INFLUX_HOST: String? = config[Key("INFLUX_HOST", stringType)],
        val INFLUX_PORT: String? = config[Key("INFLUX_PORT", stringType)],
        val INFLUX_DATABASE_NAME: String? = config[Key("INFLUX_DATABASE_NAME", stringType)],
        val INFLUX_USER: String? = config[Key("INFLUX_USER", stringType)],
        val INFLUX_PASSWORD: String? = config[Key("INFLUX_PASSWORD", stringType)],
        val grunndataApiURL: String = config[Key("GRUNNDATA_API_URL", stringType)],
    )

    data class BigQuery(
        val projectId: String = config[Key("GCP_TEAM_PROJECT_ID", stringType)],
        val datasetId: String = config[Key("BIGQUERY_DATASET_ID", stringType)],
    )
}

enum class Profile {
    LOCAL, DEV, PROD
}
