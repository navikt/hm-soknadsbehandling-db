plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization)
    implementation(libs.graphql.gradle.plugin)
    implementation(libs.ktor.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)

    // Gjør det mulig å bruke versjonskatalogen i convention plugins
    // se https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
