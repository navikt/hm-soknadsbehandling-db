import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    application
    kotlin("jvm") version "1.9.0"
    id("com.expediagroup.graphql") version "6.2.5"
    id("com.diffplug.spotless") version "6.2.0"
}

group = "no.nav.hjelpemidler.soknad.db"

repositories {
    mavenCentral()
    maven("https://jitpack.io") // Used for tms-ktor-token-support
}

application {
    applicationName = "hm-soknadsbehandling-db"
    mainClass.set("io.ktor.server.netty.EngineMain")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

fun ktor(name: String) = "io.ktor:ktor-$name:2.3.5"
fun graphqlKotlin(name: String) = "com.expediagroup:graphql-kotlin-$name:6.4.0"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Http
    implementation("no.nav.hjelpemidler.http:hm-http:v0.0.4")

    // Jackson
    val jacksonVersion = "2.15.1"
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Ktor Server
    implementation(ktor("server-core"))
    implementation(ktor("server-content-negotiation"))
    implementation(ktor("server-auth"))
    implementation(ktor("server-call-logging"))
    implementation(ktor("serialization-jackson"))

    implementation(ktor("server-auth-jwt"))
    constraints {
        implementation("com.auth0:jwks-rsa:0.22.1") {
            because("Guava vulnerable to insecure use of temporary directory (<32.0.0)")
        }
    }

    implementation(ktor("server-netty"))
    constraints {
        implementation("io.netty:netty-codec-http2:4.1.100.Final") {
            because("io.netty:netty-codec-http2 vulnerable to HTTP/2 Rapid Reset Attack #30")
        }
    }

    // Ktor Client
    implementation(ktor("client-core"))
    implementation(ktor("client-apache"))
    implementation(ktor("client-content-negotiation"))

    // Database
    implementation("org.flywaydb:flyway-core:9.21.1")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("com.github.seratch:kotliquery:1.9.0")
    implementation("org.postgresql:postgresql:42.6.0")

    // AAD og TokenX
    val tokenSupportVersion = "3.0.0"
    // implementation("com.github.navikt.tms-ktor-token-support:token-support-authentication-installer:$tokenSupportVersion")
    implementation("com.github.navikt.tms-ktor-token-support:token-support-azure-validation:$tokenSupportVersion")
    constraints {
        implementation("com.nimbusds:nimbus-jose-jwt:9.37.1") {
            because("json-smart: Uncontrolled Resource Consumption vulnerability in json-smart (Resource Exhaustion), since 9.19 has a shaded json-smart")
        }
    }

    implementation("com.github.navikt.tms-ktor-token-support:token-support-tokendings-exchange:$tokenSupportVersion")
    implementation("com.github.navikt.tms-ktor-token-support:token-support-tokenx-validation:$tokenSupportVersion")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:3.6.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.7")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.3")

    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.5")

    // InfluxDB
    implementation("org.influxdb:influxdb-java:2.23")
    constraints {
        implementation("com.squareup.okio:okio:3.6.0") {
            because("Okio Signed to Unsigned Conversion Error vulnerability")
        }
    }
    implementation("com.influxdb:influxdb-client-kotlin:6.6.0")

    // GraphQL Client
    implementation(graphqlKotlin("ktor-client")) {
        exclude("com.expediagroup", "graphql-kotlin-client-serialization") // prefer jackson
        exclude("io.ktor", "ktor-client-serialization") // prefer ktor-client-jackson
        exclude("io.ktor", "ktor-client-cio") // prefer ktor-client-apache
    }
    implementation(graphqlKotlin("client-jackson"))

    // Test
    testImplementation(kotlin("test"))
    testImplementation(ktor("server-test-host"))
    testImplementation("io.mockk:mockk:1.13.5")

    val kotestVersion = "5.5.5"
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("org.testcontainers:postgresql:1.17.6")
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
