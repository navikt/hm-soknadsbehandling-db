import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask

plugins {
    id("buildlogic.kotlin-application-conventions")
}

application {
    applicationName = "hm-soknadsbehandling-db"
    mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
    implementation(project(":behovsmeldingsmodell"))

    // DigiHoT
    implementation(libs.hm.http) {
        exclude("io.ktor", "ktor-client-cio") // prefer ktor-client-apache
    }
    implementation(libs.ktor.client.apache)

    // Ktor Server
    implementation(libs.bundles.ktor.server)

    // Jackson
    implementation(libs.bundles.jackson)

    // Database
    implementation(libs.hm.database)
    implementation(libs.hm.database) {
        capabilities {
            requireCapability("no.nav.hjelpemidler:hm-database-postgresql")
        }
    }

    // AAD og TokenX
    implementation(libs.azure.validation)
    implementation(libs.tokendings.exchange)
    implementation(libs.tokenx.validation)

    // Kafka
    implementation(libs.kafka.clients)

    // Logging
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.bundles.logging.runtime)

    // fixme -> fjern
    implementation("com.natpryce:konfig:1.6.10.0")

    // Metrics
    implementation(libs.bundles.metrics)

    // GraphQL Client
    implementation(libs.graphql.ktor.client) {
        exclude("com.expediagroup", "graphql-kotlin-client-serialization") // prefer jackson
        exclude("io.ktor", "ktor-client-serialization") // prefer ktor-client-jackson
        exclude("io.ktor", "ktor-client-cio") // prefer ktor-client-apache
    }
    implementation(libs.graphql.client.jackson)

    // Test
    testImplementation(libs.bundles.ktor.server.test)
    testImplementation(libs.wiremock)
    testImplementation(libs.hm.database) {
        capabilities {
            requireCapability("no.nav.hjelpemidler:hm-database-testcontainers")
        }
    }
}

tasks.shadowJar { mergeServiceFiles() }

graphql {
    client {
        schemaFile = file("src/main/resources/hmdb/schema.graphqls")
        queryFileDirectory = "src/main/resources/hmdb"
        packageName = "no.nav.hjelpemidler.soknad.db.client.hmdb"
    }
}

val graphqlIntrospectSchema by tasks.getting(GraphQLIntrospectSchemaTask::class) {
    endpoint.set("https://hm-grunndata-search.intern.dev.nav.no/graphql")
    outputFile.set(file("src/main/resources/hmdb/schema.graphqls"))
}

tasks.named("compileKotlin") {
    dependsOn("spotlessApply")
    dependsOn("spotlessCheck")
}
