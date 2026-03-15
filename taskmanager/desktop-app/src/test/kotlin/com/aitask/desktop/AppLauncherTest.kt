package com.aitask.desktop

import com.aitask.core.config.AppConfig
import com.aitask.core.config.ConfigLoader
import com.aitask.core.config.DatabaseConfig
import com.aitask.core.config.PoolConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertFalse

private class FakeConfigLoader : ConfigLoader {
    override fun loadAppConfig(): AppConfig = AppConfig(
        appName = "TaskManager",
        database = DatabaseConfig(
            host = "localhost",
            port = 5432,
            name = "taskmanager",
            user = "taskmanager",
            password = "taskmanager_local",
            pool = PoolConfig(
                maximumPoolSize = 2,
                minimumIdle = 1,
                connectionTimeoutMillis = 1000,
                idleTimeoutMillis = 2000,
                maxLifetimeMillis = 3000
            )
        )
    )
}

class AppLauncherTest {
    @Test
    fun `runStartupChecks should complete without errors`() {
        val launcher = AppLauncher(
            FakeConfigLoader(),
            StartupBootstrapper(),
            mapOf("BOOTSTRAP_DATABASE" to "false")
        )
        assertDoesNotThrow { launcher.runStartupChecks() }
    }

    @Test
    fun `prepareInfrastructure skips database work when disabled`() {
        val config = FakeConfigLoader().loadAppConfig()
        val result = StartupBootstrapper().prepareInfrastructure(config, false)
        assertFalse(result.databaseInitialized)
    }
}

