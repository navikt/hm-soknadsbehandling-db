import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jackson_version: String by project
val ktor_version: String by project
val graphql_client_version: String by project
val kotest_version: String by project

plugins {
    application
    kotlin("jvm") version "1.6.10"
    id("com.expediagroup.graphql") version "5.3.2"
    id("com.diffplug.spotless") version "6.2.0"
}

group = "no.nav.hjelpemidler.soknad.db"

repositories {
    mavenCentral()
}

application {
    applicationName = "hm-soknadsbehandling-db"
    mainClass.set("io.ktor.server.netty.EngineMain")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

fun ktor(name: String) = "io.ktor:ktor-$name:$ktor_version"
fun graphqlKotlin(name: String) = "com.expediagroup:graphql-kotlin-$name:$graphql_client_version"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")

    // Ktor Server
    implementation(ktor("server-core"))
    implementation(ktor("server-netty"))
    implementation(ktor("jackson"))
    implementation(ktor("auth"))
    implementation(ktor("auth-jwt"))

    // Ktor Client
    implementation(ktor("client-core"))
    implementation(ktor("client-apache"))
    implementation(ktor("client-jackson"))

    // Database
    implementation("org.flywaydb:flyway-core:8.4.3")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("com.github.seratch:kotliquery:1.6.1")
    implementation("org.postgresql:postgresql:42.3.3")

    // Logging
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.10")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.0.1")

    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.8.2")

    // InfluxDB
    implementation("org.influxdb:influxdb-java:2.22")
    implementation("com.influxdb:influxdb-client-kotlin:4.3.0")

    // GraphQL Client
    implementation(graphqlKotlin("ktor-client")) {
        exclude("com.expediagroup", "graphql-kotlin-client-serialization") // prefer jackson
        exclude("io.ktor", "ktor-client-serialization") // prefer ktor-client-jackson
        exclude("io.ktor", "ktor-client-cio") // prefer ktor-client-apache
    }
    implementation(graphqlKotlin("client-jackson"))

    // BigQuery
    implementation("com.google.cloud:google-cloud-bigquery:2.10.0")

    // Test
    testImplementation(kotlin("test"))
    testImplementation(ktor("server-test-host"))
    testImplementation("io.mockk:mockk:1.12.2")
    testImplementation("io.kotest:kotest-assertions-core:$kotest_version")
    testImplementation("io.kotest:kotest-runner-junit5:$kotest_version")
    testImplementation("org.testcontainers:postgresql:1.16.3")
    testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
}

spotless {
    kotlin {
        ktlint()
        targetExclude("**/generated/**")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("spotlessApply")
    dependsOn("spotlessCheck")

    kotlinOptions.jvmTarget = "17"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
    from(
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    )
}

graphql {
    client {
        schemaFile = file("src/main/resources/hmdb/schema.graphql")
        queryFileDirectory = "src/main/resources/hmdb"
        packageName = "no.nav.hjelpemidler.soknad.db.client.hmdb"
    }
}

val graphqlIntrospectSchema by tasks.getting(GraphQLIntrospectSchemaTask::class) {
    endpoint.set("https://hm-grunndata-api.dev.intern.nav.no/graphql")
    outputFile.set(file("src/main/resources/hmdb/schema.graphql"))
}
