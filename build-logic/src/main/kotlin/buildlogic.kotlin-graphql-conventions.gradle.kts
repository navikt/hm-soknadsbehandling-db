import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("buildlogic.kotlin-common-conventions")
    id("com.expediagroup.graphql")
}

// Gjør det mulig å bruke versjonskatalogen i convention plugins
// se https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
val libs = the<LibrariesForLibs>()

dependencies {
    api(libs.graphql.ktor.client) {
        exclude("com.expediagroup", "graphql-kotlin-client-serialization") // prefer jackson
        exclude("io.ktor", "ktor-client-serialization") // prefer ktor-client-jackson
        exclude("io.ktor", "ktor-client-cio") // prefer ktor-client-apache
    }
    api(libs.graphql.client.jackson)
}
