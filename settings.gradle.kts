rootProject.name = "hm-soknadsbehandling-db"
include("app")

sourceControl {
    gitRepository(uri("https://github.com/navikt/hm-http.git")) {
        producesModule("no.nav.hjelpemidler.http:hm-http")
    }
}
