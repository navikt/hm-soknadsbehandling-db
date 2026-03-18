val hotlibsKatalogVersion: String by settings

pluginManagement {
    includeBuild("build-logic")
}

fun RepositoryHandler.github(repository: String) {
    maven {
        url = uri("https://maven.pkg.github.com/$repository")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()

        github("navikt/hotlibs")
        github("navikt/tms-ktor-token-support")

        // Plassert under GitHub-repositories (med authentication) for å unngå unødvendige kostnader.
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.hjelpemidler:katalog:$hotlibsKatalogVersion")
        }
    }
}

rootProject.name = "hm-soknadsbehandling-db"
include("app")
