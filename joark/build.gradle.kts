import com.expediagroup.graphql.plugin.gradle.config.GraphQLScalar

plugins {
    id("buildlogic.kotlin-graphql-conventions")
}

dependencies {
    implementation(libs.kotlin.logging)
    implementation(libs.hotlibs.http)
}

graphql {
    client {
        schemaFile = file("safselvbetjening-api-sdl.graphqls")
        packageName = "no.nav.hjelpemidler.soknad.db.client.safselvbetjening"
        customScalars =
            listOf(
                GraphQLScalar(
                    "DateTime",
                    "java.time.OffsetDateTime",
                    "no.nav.hjelpemidler.soknad.db.client.safselvbetjening.DateTimeScalarConverter",
                ),
            )
    }
}
