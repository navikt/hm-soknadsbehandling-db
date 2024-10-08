plugins {
    id("buildlogic.kotlin-library-conventions")
    `maven-publish`
    `java-test-fixtures`
}

group = "no.nav.hjelpemidler"
version = System.getenv("VERSION_TAG") ?: "local"

dependencies {
    api(libs.hotlibs.core)

    // Logging
    implementation(libs.slf4j.api)

    /**
     * OWASP
     *
     * TODO: Det tryggeste er å ha denne avhengigheten her, slik at vi kan sikre at den alltid blir brukt,
     * men samtidig vi vil helst ikke ha ekstra avhengigheter her. Kan vi gjøre det på en bedre måte?
     */
    implementation(libs.owasp.java.html.sanitizer)

    testImplementation(libs.bundles.test)
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
