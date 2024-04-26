import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.graphql)
    // alias(libs.plugins.spotless)
}

application {
    applicationName = "hm-soknadsbehandling-db"
    mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
    implementation(libs.kotlin.stdlib)

    // DigiHoT
    implementation(libs.hm.http)

    // Jackson
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)

    // Ktor Server
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    // Database
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.kotliquery)

    // AAD og TokenX
    implementation(libs.azure.validation)
    implementation(libs.tokendings.exchange)
    implementation(libs.tokenx.validation)

    // Kafka
    implementation(libs.kafka.clients)

    // Logging
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.logstash.logback.encoder)

    // fixme -> fjern
    implementation("com.natpryce:konfig:1.6.10.0")

    // Metrics
    implementation(libs.influxdb.client.kotlin)
    implementation(libs.micrometer.registry.prometheus)

    // GraphQL Client
    implementation(libs.graphql.ktor.client) {
        exclude("com.expediagroup", "graphql-kotlin-client-serialization") // prefer jackson
        exclude("io.ktor", "ktor-client-serialization") // prefer ktor-client-jackson
    }
    implementation(libs.graphql.client.jackson)

    // Test
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.wiremock)
}

//spotless {
//    kotlin {
//        ktlint()
//        targetExclude("**/generated/**")
//    }
//    kotlinGradle {
//        target("*.gradle.kts")
//        ktlint()
//    }
//}

kotlin { jvmToolchain(21) }

tasks.test { useJUnitPlatform() }
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
