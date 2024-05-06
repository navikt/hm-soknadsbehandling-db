import gradle.kotlin.dsl.accessors._0eff37d95dcaaf81e6e5b447e8f4d697.implementation
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("buildlogic.kotlin-common-conventions")
    `java-library`
}

// Gjør det mulig å bruke versjonskatalogen i convention plugins
// se https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
val libs = the<LibrariesForLibs>()

dependencies {
    implementation(libs.jackson.annotations)
}
