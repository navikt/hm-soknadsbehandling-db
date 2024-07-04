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

        "NAIS_APP_NAME" to "hm-soknadsbehandling-db",
        "NAIS_CLUSTER_NAME" to "dev-gcp",
        "NAIS_NAMESPACE" to "teamdigihot",

        "INFLUX_HOST" to "http://localhost",
        "INFLUX_PORT" to "1234",
        "INFLUX_DATABASE_NAME" to "defaultdb",
        "INFLUX_USER" to "user",
        "INFLUX_PASSWORD" to "password",

        "userclaim" to "sub",

        "GRUNNDATA_API_URL" to "https://hm-grunndata-search.intern.dev.nav.no",
        "HM_ROLLER_URL" to "https://localhost",
        "HM_ROLLER_AUDIENCE" to "audience",
        "BIGQUERY_DATASET_ID" to "hm_soknadsbehandling_v1_dataset_local",

        "GCP_TEAM_PROJECT_ID" to "teamdigihot",
    ),
)
private val devProperties = ConfigurationMap(
    mapOf(
        "userclaim" to "pid",

        "GRUNNDATA_API_URL" to "http://hm-grunndata-search",
        "HM_ROLLER_URL" to "http://hm-roller",
        "HM_ROLLER_AUDIENCE" to "dev-gcp:teamdigihot:hm-roller",
        "BIGQUERY_DATASET_ID" to "hm_soknadsbehandling_v1_dataset_dev",
    ),
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "userclaim" to "pid",

        "GRUNNDATA_API_URL" to "http://hm-grunndata-search",
        "HM_ROLLER_URL" to "http://hm-roller",
        "HM_ROLLER_AUDIENCE" to "prod-gcp:teamdigihot:hm-roller",
        "BIGQUERY_DATASET_ID" to "hm_soknadsbehandling_v1_dataset_prod",
    ),
)

private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-gcp" -> systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-gcp" -> systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> {
        systemProperties() overriding EnvironmentVariables overriding localProperties
    }
}

object Configuration {
    val config = config()
    val application: Application = Application()
    val kafka: Kafka by lazy { Kafka() }

    data class Application(
        val id: String = config.getOrElse(Key("", stringType), "hm-soknadsbehandling-db-v1"),
        val userclaim: String = config[Key("userclaim", stringType)],
        val NAIS_APP_NAME: String? = config[Key("NAIS_APP_NAME", stringType)],
        val NAIS_CLUSTER_NAME: String? = config[Key("NAIS_CLUSTER_NAME", stringType)],
        val NAIS_NAMESPACE: String? = config[Key("NAIS_NAMESPACE", stringType)],
        val INFLUX_HOST: String? = config[Key("INFLUX_HOST", stringType)],
        val INFLUX_PORT: String? = config[Key("INFLUX_PORT", stringType)],
        val INFLUX_DATABASE_NAME: String? = config[Key("INFLUX_DATABASE_NAME", stringType)],
        val INFLUX_USER: String? = config[Key("INFLUX_USER", stringType)],
        val INFLUX_PASSWORD: String? = config[Key("INFLUX_PASSWORD", stringType)],
        val grunndataApiURL: String = config[Key("GRUNNDATA_API_URL", stringType)],
        val hmRollerUrl: String = config[Key("HM_ROLLER_URL", stringType)],
        val hmRollerAudience: String = config[Key("HM_ROLLER_AUDIENCE", stringType)],
    )

    data class Kafka(
        val topic: String = config.getOrElse(Key("KAFKA_TOPIC", stringType), "teamdigihot.hm-soknadsbehandling-v1"),
        val brokers: String = config[Key("KAFKA_BROKERS", stringType)],
        val truststore_path: String = config[Key("KAFKA_TRUSTSTORE_PATH", stringType)],
        val keystore_path: String = config[Key("KAFKA_KEYSTORE_PATH", stringType)],
        val credstore_password: String = config[Key("KAFKA_CREDSTORE_PASSWORD", stringType)],
    )
}
