plugins {
    id("buildlogic.kotlin-library-conventions")
    `maven-publish`
}

group = "no.nav.hjelpemidler"
version = System.getenv("GITHUB_REF_NAME") ?: "local"

dependencies {
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
