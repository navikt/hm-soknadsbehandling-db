package no.nav.hjelpemidler.soknad.db.db

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.hjelpemidler.database.Testcontainers
import no.nav.hjelpemidler.database.createDataSource
import no.nav.hjelpemidler.soknad.db.Configuration
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test

internal object DataSource {
    val instance: javax.sql.DataSource by lazy {
        createDataSource(Testcontainers) { tag = "15-alpine" }
            .also { sessionOf(it).run(queryOf("DROP ROLE IF EXISTS cloudsqliamuser").asExecute) }
            .also { sessionOf(it).run(queryOf("CREATE ROLE cloudsqliamuser").asExecute) }
    }
}

internal fun withCleanDb(test: () -> Unit) = DataSource.instance.also { clean(it) }
    .run { test() }

internal fun withMigratedDb(test: () -> Unit) =
    DataSource.instance.also { clean(it) }
        .also { migrate(it) }.run { test() }

internal class PostgresTest {
    @Test
    fun `JDBC url is set correctly from  config values `() {
        with(hikariConfigFrom(Configuration)) {
            jdbcUrl shouldBe "jdbc:postgresql://host.docker.internal:5434/soknadsbehandling"
        }
    }
}

private fun clean(dataSource: javax.sql.DataSource) =
    Flyway.configure().cleanDisabled(false).dataSource(dataSource).load().clean()
