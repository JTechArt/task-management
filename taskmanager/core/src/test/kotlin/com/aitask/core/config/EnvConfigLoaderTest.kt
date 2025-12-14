package com.aitask.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvConfigLoaderTest {
    @Test
    fun `loadAppConfig uses defaults when env is empty`() {
        val loader = EnvConfigLoader(emptyMap())
        val config = loader.loadAppConfig()
        assertEquals("TaskManager", config.appName)
        assertEquals("localhost", config.database.host)
        assertEquals(5432, config.database.port)
        assertTrue(config.database.password.isNotBlank())
    }

    @Test
    fun `loadAppConfig applies environment overrides`() {
        val env = mapOf(
            "APP_NAME" to "AiTask",
            "DB_HOST" to "db",
            "DB_PORT" to "5555",
            "DB_NAME" to "aitask",
            "DB_USER" to "ai",
            "DB_PASSWORD" to "secret",
            "DB_MAX_POOL_SIZE" to "5",
            "DB_MIN_IDLE" to "1",
            "DB_CONNECTION_TIMEOUT" to "1000",
            "DB_IDLE_TIMEOUT" to "2000",
            "DB_MAX_LIFETIME" to "3000"
        )
        val loader = EnvConfigLoader(env)
        val config = loader.loadAppConfig()
        assertEquals("AiTask", config.appName)
        assertEquals("db", config.database.host)
        assertEquals(5555, config.database.port)
        assertEquals("aitask", config.database.name)
        assertEquals("ai", config.database.user)
        assertEquals("secret", config.database.password)
        assertEquals(5, config.database.pool.maximumPoolSize)
        assertEquals(1, config.database.pool.minimumIdle)
        assertEquals(1000, config.database.pool.connectionTimeoutMillis)
        assertEquals(2000, config.database.pool.idleTimeoutMillis)
        assertEquals(3000, config.database.pool.maxLifetimeMillis)
    }
}

