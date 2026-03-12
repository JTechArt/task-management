package com.aitask.core.db

import com.aitask.core.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun createDataSource(config: DatabaseConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.buildJdbcUrl()
            username = config.user
            password = config.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = config.pool.maximumPoolSize
            minimumIdle = config.pool.minimumIdle
            connectionTimeout = config.pool.connectionTimeoutMillis
            idleTimeout = config.pool.idleTimeoutMillis
            maxLifetime = config.pool.maxLifetimeMillis
            isAutoCommit = false
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        return HikariDataSource(hikariConfig)
    }

    fun connect(dataSource: HikariDataSource): Database = Database.connect(dataSource)

    fun migrate(dataSource: HikariDataSource): Int {
        val result = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        return result.migrationsExecuted
    }
}

