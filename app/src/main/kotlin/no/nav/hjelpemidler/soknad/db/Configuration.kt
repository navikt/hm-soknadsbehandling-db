package no.nav.hjelpemidler.soknad.db

import no.nav.hjelpemidler.configuration.EnvironmentVariable
import no.nav.hjelpemidler.configuration.environmentVariable

object Configuration {
    val PORT: Int by environmentVariable()

    val KAFKA_TOPIC by EnvironmentVariable

    val GRUNNDATA_API_URL by EnvironmentVariable

    val HM_ROLLER_URL by EnvironmentVariable
    val HM_ROLLER_AUDIENCE by EnvironmentVariable

    val BIGQUERY_DATASET_ID by EnvironmentVariable // fixme -> brukes ikke
}
