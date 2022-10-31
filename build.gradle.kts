import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    application
    kotlin("jvm") version "1.6.10"
    id("com.expediagroup.graphql") version "6.2.5"
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

fun ktor(name: String) = "io.ktor:ktor-$name:2.1.2"
fun graphqlKotlin(name: String) = "com.expediagroup:graphql-kotlin-$name:6.2.5"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Jackson
    val jacksonVersion = "2.13.4"
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion.2") // .2 pga snyk, kan sikkert ha samme versjon som de to andre senere
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Ktor Server
    implementation(ktor("server-core"))
    implementation(ktor("server-netty"))
    implementation(ktor("server-content-negotiation"))
    implementation(ktor("server-auth"))
    implementation(ktor("server-auth-jwt"))
    implementation(ktor("server-call-logging"))
    implementation(ktor("serialization-jackson"))

    // Ktor Client
    implementation(ktor("client-core"))
    implementation(ktor("client-apache"))
    implementation(ktor("client-content-negotiation"))

    // Database
    implementation("org.flywaydb:flyway-core:9.5.1")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("com.github.seratch:kotliquery:1.9.0")
    implementation("org.postgresql:postgresql:42.5.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:3.0.2")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.4")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.2")

    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.9.5")

    // InfluxDB
    implementation("org.influxdb:influxdb-java:2.23")
    implementation("com.influxdb:influxdb-client-kotlin:6.6.0")

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
    testImplementation("io.mockk:mockk:1.13.2")

    val kotestVersion = "5.5.2"
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("org.testcontainers:postgresql:1.17.4")
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
