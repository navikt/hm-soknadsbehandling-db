import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask

plugins {
    id("buildlogic.kotlin-application-conventions")
}

application {
    applicationName = "hm-soknadsbehandling-db"
    mainClass.set("no.nav.hjelpemidler.soknad.db.ApplicationKt")
}

dependencies {
    implementation(project(":behovsmeldingsmodell"))

    // hotlibs
    implementation(libs.hotlibs.core)
    implementation(libs.hotlibs.http) { exclude("io.ktor", "ktor-client-cio") } // prefer ktor-client-apache
    implementation(libs.hotlibs.kafka)
    implementation(libs.hotlibs.logging)
    implementation(libs.hotlibs.serialization)

    // Ktor Client
    implementation(libs.ktor.client.apache)

    // Ktor Server
    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.server.resources)

    // Database
    implementation(libs.hotlibs.database) {
        capabilities {
            requireCapability("no.nav.hjelpemidler:database-postgresql")
        }
    }

    // AAD og TokenX
    implementation(libs.azure.validation)
    implementation(libs.tokendings.exchange)
    implementation(libs.tokenx.validation)

    // Metrics
    implementation(libs.micrometer.registry.prometheus)

    // GraphQL Client
    implementation(libs.graphql.ktor.client) {
        exclude("com.expediagroup", "graphql-kotlin-client-serialization") // prefer jackson
        exclude("io.ktor", "ktor-client-serialization") // prefer ktor-client-jackson
        exclude("io.ktor", "ktor-client-cio") // prefer ktor-client-apache
    }
    implementation(libs.graphql.client.jackson)
}

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

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            dependencies {
                implementation(libs.kotest.assertions.ktor)
                implementation(libs.ktor.client.resources)
                implementation(libs.ktor.server.test.host)
                implementation(libs.wiremock)
                implementation(libs.hotlibs.database) {
                    capabilities {
                        requireCapability("no.nav.hjelpemidler:database-testcontainers")
                    }
                }
            }
        }
    }
}

tasks {
    named("compileKotlin") {
        dependsOn("spotlessApply")
        dependsOn("spotlessCheck")
    }
    shadowJar { mergeServiceFiles() }
}
