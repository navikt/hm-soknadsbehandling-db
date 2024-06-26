import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.diffplug.spotless")
}

// Gjør det mulig å bruke versjonskatalogen i convention plugins
// se https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
val libs = the<LibrariesForLibs>()

dependencies {
    implementation(libs.kotlin.stdlib)
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(21) }

spotless {
    kotlin {
        ktlint().editorConfigOverride(
            mapOf(
                "ktlint_standard_max-line-length" to "disabled",
                "ktlint_standard_value-argument-comment" to "disabled",
                "ktlint_standard_value-parameter-comment" to "disabled",
            ),
        )
        targetExclude("**/generated/**")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}
