plugins {
    id("buildlogic.kotlin-library-conventions")
    `maven-publish`
}

group = "no.nav.hjelpemidler"
version = System.getenv("VERSION_TAG") ?: "local"

dependencies {
    api(libs.hotlibs.core)
    /*
     TODO: Det tryggeste er å ha denne avhengigheten her, slik at vi kan sikre at den alltid blir brukt,
      men samtidig vi vil helst ikke ha ekstra avhengigheter her. Kan vi gjøre det på en bedre måte?
     */

    // TODO: legg til i hm-katalog

    // OWASP
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1")

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

tasks.named("compileKotlin") {
    dependsOn("spotlessApply")
    dependsOn("spotlessCheck")
}
