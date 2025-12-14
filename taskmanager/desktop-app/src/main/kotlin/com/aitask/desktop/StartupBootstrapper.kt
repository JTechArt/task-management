package com.aitask.desktop

import com.aitask.core.config.AppConfig
import com.aitask.core.db.DatabaseFactory
import mu.KotlinLogging

private val bootstrapLogger = KotlinLogging.logger {}

data class BootstrapResult(
    val databaseInitialized: Boolean
)

class StartupBootstrapper {
    fun prepareInfrastructure(config: AppConfig, enableDatabase: Boolean): BootstrapResult {
        if (!enableDatabase) {
            bootstrapLogger.info { "Database bootstrap skipped (BOOTSTRAP_DATABASE=false)" }
            return BootstrapResult(databaseInitialized = false)
        }
        val dataSource = DatabaseFactory.createDataSource(config.database)
        DatabaseFactory.migrate(dataSource)
        DatabaseFactory.connect(dataSource)
        bootstrapLogger.info { "Database bootstrap completed" }
        return BootstrapResult(databaseInitialized = true)
    }
}

