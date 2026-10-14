plugins {
    id("buildlogic.kotlin-graphql-conventions")
}

dependencies {
    implementation(libs.kotlin.logging)
    implementation(libs.hotlibs.http)
}

graphql {
    client {
        schemaFile = file("grunndata-api-sdl.graphqls")
        packageName = "no.nav.hjelpemidler.soknad.db.grunndata"
    }
}
