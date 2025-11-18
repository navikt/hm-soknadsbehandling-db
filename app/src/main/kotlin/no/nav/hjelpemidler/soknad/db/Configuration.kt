package no.nav.hjelpemidler.soknad.db

import no.nav.hjelpemidler.configuration.EnvironmentVariable

object Configuration {
    val PORT by EnvironmentVariable(transform = String::toInt)

    val ELECTOR_GET_URL by EnvironmentVariable

    val KAFKA_TOPIC by EnvironmentVariable

    val GRUNNDATA_API_URL by EnvironmentVariable

    val HM_ROLLER_URL by EnvironmentVariable
    val HM_ROLLER_AUDIENCE by EnvironmentVariable

    val SAFSELVBETJENING_URL by EnvironmentVariable
    val SAFSELVBETJENING_AUDIENCE by EnvironmentVariable

    val BIGQUERY_DATASET_ID by EnvironmentVariable // fixme -> brukes ikke
}
