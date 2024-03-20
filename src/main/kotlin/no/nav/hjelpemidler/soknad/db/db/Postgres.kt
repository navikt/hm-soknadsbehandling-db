package no.nav.hjelpemidler.soknad.db.db

import com.fasterxml.jackson.databind.JsonNode
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import mu.KotlinLogging
import no.nav.hjelpemidler.soknad.db.Configuration
import no.nav.hjelpemidler.soknad.db.JacksonMapper
import no.nav.hjelpemidler.soknad.db.Profile
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.intellij.lang.annotations.Language
import java.net.Socket
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

@ExperimentalTime
internal fun waitForDB(timeout: Duration, config: Configuration): Boolean {
    val deadline = LocalDateTime.now().plusSeconds(timeout.inWholeSeconds)
    while (true) {
        try {
            Socket(config.database.host, config.database.port.toInt())
            return true
        } catch (e: Exception) {
            logger.info("Database not available yet, waiting...")
            Thread.sleep(2.seconds.inWholeMilliseconds)
        }
        if (LocalDateTime.now().isAfter(deadline)) break
    }
    return false
}

fun Row.jsonNode(columnLabel: String): JsonNode =
    when (val content = stringOrNull(columnLabel)) {
        null -> JacksonMapper.objectMapper.nullNode()
        else -> JacksonMapper.objectMapper.readTree(content)
    }

fun Row.jsonNodeOrNull(columnLabel: String): JsonNode? = stringOrNull(columnLabel)?.let {
    JacksonMapper.objectMapper.readTree(it)
}

fun Row.jsonNodeOrDefault(columnLabel: String, @Language("JSON") defaultContent: String): JsonNode =
    when (val content = stringOrNull(columnLabel)) {
        null -> JacksonMapper.objectMapper.readTree(defaultContent)
        else -> JacksonMapper.objectMapper.readTree(content)
    }

internal fun migrate(config: Configuration) =
    HikariDataSource(hikariConfigFrom(config)).use { migrate(it) }

internal fun hikariConfigFrom(config: Configuration) =
    HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://${config.database.host}:${config.database.port}/${config.database.name}"
        maximumPoolSize = 20
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
        config.database.user?.let { username = it }
        config.database.password?.let { password = it }
    }

internal fun dataSourceFrom(config: Configuration): HikariDataSource = when (config.application.profile) {
    Profile.LOCAL -> HikariDataSource(hikariConfigFrom(config))
    else -> HikariDataSource(hikariConfigFrom(config))
}

internal fun migrate(dataSource: HikariDataSource, initSql: String = ""): MigrateResult =
    Flyway.configure().dataSource(dataSource).initSql(initSql).load().migrate()
