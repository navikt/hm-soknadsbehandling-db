plugins {
    id("buildlogic.kotlin-application-conventions")

    alias(libs.plugins.flyway)
}

application {
    applicationName = "hm-soknadsbehandling-db"
    mainClass = "no.nav.hjelpemidler.soknad.db.ApplicationKt"
}

dependencies {
    implementation(project(":grunndata"))
    implementation(project(":joark"))

    // hotlibs
    implementation(libs.hotlibs.behovsmelding)
    implementation(libs.hotlibs.core)
    implementation(libs.hotlibs.http) {
        exclude("io.ktor", "ktor-client-cio")
    } // prefer ktor-client-apache
    implementation(libs.hotlibs.kafka)
    implementation(libs.hotlibs.logging)
    implementation(libs.hotlibs.serialization)
    implementation(libs.hotlibs.server)

    // Ktor Client
    implementation(libs.ktor.client.apache5)

    // Ktor Server
    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.server.di)
    implementation(libs.ktor.server.resources)

    // Database
    implementation(libs.hotlibs.database) {
        capabilities {
            requireCapability("no.nav.hjelpemidler:database-postgresql")
        }
    }

    // Metrics
    implementation(libs.micrometer.registry.prometheus)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        @Suppress("UnstableApiUsage")
        val test =
            named<JvmTestSuite>("test") {
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
                    implementation(testFixtures(libs.hotlibs.server))
                }
            }
    }
}

tasks {
    named("compileKotlin") {
        dependsOn("spotlessApply")
        dependsOn("spotlessCheck")
    }
    shadowJar {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

buildscript {
    dependencies {
        classpath(libs.flyway.database.postgresql)
    }
}

flyway {
    url = "jdbc:postgresql://localhost:5432/soknadsbehandling?reWriteBatchedInserts=true"
    user = System.getenv("FLYWAY_USER")
    password = System.getenv("FLYWAY_PASSWORD")
    cleanDisabled = false
}
