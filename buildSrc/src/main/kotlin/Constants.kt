
object Database {
    const val Postgres = "org.postgresql:postgresql:42.2.11"
    const val Kotlinquery = "com.github.seratch:kotliquery:1.3.1"
    const val Flyway = "org.flywaydb:flyway-core:6.3.2"
    const val HikariCP = "com.zaxxer:HikariCP:3.4.1"
}

object Fuel {
    const val version = "2.2.1"
    const val fuel = "com.github.kittinunf.fuel:fuel:$version"
    const val fuelMoshi = "com.github.kittinunf.fuel:fuel-moshi:$version"
    fun library(name: String) = "com.github.kittinunf.fuel:fuel-$name:$version"
}

object GradleWrapper {
    const val version = "5.5"
}

object Jackson {
    const val version = "2.10.3"
    const val core = "com.fasterxml.jackson.core:jackson-core:$version"
    const val kotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:$version"
    const val jsr310 = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$version"
}

object Junit5 {
    const val version = "5.6.1"
    const val api = "org.junit.jupiter:junit-jupiter-api:$version"
    const val params = "org.junit.jupiter:junit-jupiter-params:$version"
    const val engine = "org.junit.jupiter:junit-jupiter-engine:$version"
    const val vintageEngine = "org.junit.vintage:junit-vintage-engine:$version"
    fun library(name: String) = "org.junit.jupiter:junit-jupiter-$name:$version"
}

object Json {
    const val version = "20180813"
    const val library = "org.json:json:$version"
}

object JsonAssert {
    const val version = "1.5.0"
    const val jsonassert = "org.skyscreamer:jsonassert:$version"
}

object Klint {
    const val version = "0.33.0"
}

object Konfig {
    const val konfig = "com.natpryce:konfig:1.6.10.0"
}

object Kotlin {
    const val version = "1.4.0"
    const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:$version"

    object Coroutines {
        const val version = "1.3.7"
        fun module(name: String) = "org.jetbrains.kotlinx:kotlinx-coroutines-$name:$version"
    }

    object Logging {
        const val version = "1.7.9"
        const val kotlinLogging = "io.github.microutils:kotlin-logging:$version"
    }
}

object KoTest {
    const val version = "4.2.0.RC2"

    // for kotest framework
    const val runner = "io.kotest:kotest-runner-junit5-jvm:$version"

    // for kotest core jvm assertion
    const val assertions = "io.kotest:kotest-assertions-core-jvm:$version"

    // for kotest property test
    const val property = "io.kotest:kotest-property-jvm:$version"

    // any other library
    fun library(name: String) = "io.kotest:kotest-$name:$version"
}

object Kotlinx {
    const val bimap = "com.uchuhimo:kotlinx-bimap:1.2"
}

object Ktor {
    const val version = "1.4.0"
    const val server = "io.ktor:ktor-server:$version"
    const val serverNetty = "io.ktor:ktor-server-netty:$version"
    const val auth = "io.ktor:ktor-auth:$version"
    const val authJwt = "io.ktor:ktor-auth-jwt:$version"
    const val locations = "io.ktor:ktor-locations:$version"
    const val micrometerMetrics = "io.ktor:ktor-metrics-micrometer:$version"
    const val ktorTest = "io.ktor:ktor-server-test-host:$version"
    fun library(name: String) = "io.ktor:ktor-$name:$version"
}

object Log4j2 {
    const val version = "2.13.1"
    const val api = "org.apache.logging.log4j:log4j-api:$version"
    const val core = "org.apache.logging.log4j:log4j-core:$version"
    const val slf4j = "org.apache.logging.log4j:log4j-slf4j-impl:$version"

    fun library(name: String) = "org.apache.logging.log4j:log4j-$name:$version"

    object Logstash {
        private const val version = "1.0.2"
        const val logstashLayout = "com.vlkan.log4j2:log4j2-logstash-layout:$version"
    }
}

object Micrometer {
    const val version = "1.4.0"
    const val prometheusRegistry = "io.micrometer:micrometer-registry-prometheus:$version"
}

object Mockk {
    const val version = "1.10.0"
    const val mockk = "io.mockk:mockk:$version"
}

object Prometheus {
    const val version = "0.8.1"
    const val common = "io.prometheus:simpleclient_common:$version"
    const val hotspot = "io.prometheus:simpleclient_hotspot:$version"
    const val log4j2 = "io.prometheus:simpleclient_log4j2:$version"
    fun library(name: String) = "io.prometheus:simpleclient_$name:$version"

    object Nare {
        const val version = "0b41ab4"
        const val prometheus = "no.nav:nare-prometheus:$version"
    }
}

object Slf4j {
    const val version = "1.7.25"
    const val api = "org.slf4j:slf4j-api:$version"
}

object Ktlint {
    const val version = "0.38.1"
}

object Spotless {
    const val version = "5.1.0"
    const val spotless = "com.diffplug.spotless"
}

object Shadow {
    const val version = "5.2.0"
    const val shadow = "com.github.johnrengelman.shadow"
}

object TestContainers {
    const val version = "1.15.1"
    const val postgresql = "org.testcontainers:postgresql:$version"
    const val kafka = "org.testcontainers:kafka:$version"
}

object Ulid {
    const val version = "8.2.0"
    const val ulid = "de.huxhorn.sulky:de.huxhorn.sulky.ulid:$version"
}

object Wiremock {
    const val version = "2.21.0"
    const val standalone = "com.github.tomakehurst:wiremock-standalone:$version"
}

object Graphql {
    const val version = "3.1.1"
    const val graphql = "com.expediagroup.graphql"
    val client = library("client")
    fun library(name: String) = "com.expediagroup:graphql-kotlin-$name:$version"
}
