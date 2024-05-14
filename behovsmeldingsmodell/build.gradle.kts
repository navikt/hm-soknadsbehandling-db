plugins {
    id("buildlogic.kotlin-library-conventions")
    `maven-publish`
}

group = "no.nav.hjelpemidler"
version = System.getenv("VERSION_TAG") ?: "local"

dependencies {
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.bundles.jackson)
}

java { withSourcesJar() }

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "hm-behovsmeldingsmodell"
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/navikt/hm-soknadsbehandling-db")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
