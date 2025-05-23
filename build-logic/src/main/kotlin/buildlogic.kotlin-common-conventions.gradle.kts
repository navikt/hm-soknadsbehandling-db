import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.diffplug.spotless")
}

// Gjør det mulig å bruke versjonskatalogen i convention plugins
// se https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
val libs = the<LibrariesForLibs>()

dependencies {
    implementation(platform(libs.hotlibs.platform))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest(libs.versions.kotlin.asProvider())
            dependencies {
                implementation(libs.hotlibs.test)
            }

            targets.all {
                testTask {
                    environment(
                        "NAIS_CLUSTER_NAME" to "test",
                    )
                }
            }
        }
    }
}

spotless {
    kotlin {
        ktlint().editorConfigOverride(
            mapOf(
                "ktlint_code_style" to "intellij_idea",
                "ktlint_standard_function-expression-body" to "disabled",
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
