package com.aitask.desktop

import com.aitask.core.config.AppConfig
import com.aitask.core.db.DatabaseFactory
import io.github.oshai.kotlinlogging.KotlinLogging

private val bootstrapLogger = KotlinLogging.logger {}

data class BootstrapResult(
    val databaseInitialized: Boolean,
    val databaseError: String? = null,
    val migrationsApplied: Int = 0
)

class StartupBootstrapper {
    fun prepareInfrastructure(config: AppConfig, enableDatabase: Boolean): BootstrapResult {
        if (!enableDatabase) {
            bootstrapLogger.info { "Database bootstrap skipped (BOOTSTRAP_DATABASE=false)" }
            return BootstrapResult(
                databaseInitialized = false,
                databaseError = "Database bootstrap disabled"
            )
        }

        return try {
            bootstrapLogger.info { "Starting database initialization" }
            val dataSource = DatabaseFactory.createDataSource(config.database)

            bootstrapLogger.info { "Running database migrations" }
            val migrationsApplied = DatabaseFactory.migrate(dataSource)
            bootstrapLogger.info { "Applied $migrationsApplied migrations" }

            DatabaseFactory.connect(dataSource)
            bootstrapLogger.info { "Database bootstrap completed successfully" }

            BootstrapResult(
                databaseInitialized = true,
                migrationsApplied = migrationsApplied
            )
        } catch (e: Exception) {
            bootstrapLogger.error(e) { "Database bootstrap failed: ${e.message}" }
            BootstrapResult(
                databaseInitialized = false,
                databaseError = e.message ?: "Unknown database error"
            )
        }
    }
}

