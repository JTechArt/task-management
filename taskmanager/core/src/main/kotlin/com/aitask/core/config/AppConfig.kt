package com.aitask.core.config

import kotlinx.serialization.Serializable

private const val DEFAULT_APP_NAME: String = "TaskManager"
private const val DEFAULT_DB_HOST: String = "localhost"
private const val DEFAULT_DB_PORT: Int = 5432
private const val DEFAULT_DB_NAME: String = "taskmanager"
private const val DEFAULT_DB_USER: String = "taskmanager"
private const val DEFAULT_DB_PASSWORD: String = "taskmanager_local"
private const val DEFAULT_MAX_POOL_SIZE: Int = 10
private const val DEFAULT_MIN_IDLE: Int = 2
private const val DEFAULT_CONNECTION_TIMEOUT: Long = 30000
private const val DEFAULT_IDLE_TIMEOUT: Long = 600000
private const val DEFAULT_MAX_LIFETIME: Long = 1800000

@Serializable
data class PoolConfig(
    val maximumPoolSize: Int,
    val minimumIdle: Int,
    val connectionTimeoutMillis: Long,
    val idleTimeoutMillis: Long,
    val maxLifetimeMillis: Long
)

@Serializable
data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
    val pool: PoolConfig
) {
    fun buildJdbcUrl(): String = "jdbc:postgresql://$host:$port/$name"
    fun redactSensitiveValues(): Map<String, Any> = mapOf(
        "host" to host,
        "port" to port,
        "name" to name,
        "user" to user,
        "pool" to pool
    )
}

@Serializable
data class AppConfig(
    val appName: String,
    val database: DatabaseConfig
)

interface ConfigLoader {
    fun loadAppConfig(): AppConfig
}

class EnvConfigLoader(
    private val environment: Map<String, String> = System.getenv()
) : ConfigLoader {
    override fun loadAppConfig(): AppConfig {
        val poolConfig = PoolConfig(
            maximumPoolSize = readInt("DB_MAX_POOL_SIZE", DEFAULT_MAX_POOL_SIZE),
            minimumIdle = readInt("DB_MIN_IDLE", DEFAULT_MIN_IDLE),
            connectionTimeoutMillis = readLong("DB_CONNECTION_TIMEOUT", DEFAULT_CONNECTION_TIMEOUT),
            idleTimeoutMillis = readLong("DB_IDLE_TIMEOUT", DEFAULT_IDLE_TIMEOUT),
            maxLifetimeMillis = readLong("DB_MAX_LIFETIME", DEFAULT_MAX_LIFETIME)
        )
        val databaseConfig = DatabaseConfig(
            host = readString("DB_HOST", DEFAULT_DB_HOST),
            port = readInt("DB_PORT", DEFAULT_DB_PORT),
            name = readString("DB_NAME", DEFAULT_DB_NAME),
            user = readString("DB_USER", DEFAULT_DB_USER),
            password = readString("DB_PASSWORD", DEFAULT_DB_PASSWORD),
            pool = poolConfig
        )
        return AppConfig(
            appName = readString("APP_NAME", DEFAULT_APP_NAME),
            database = databaseConfig
        )
    }

    private fun readString(name: String, defaultValue: String): String = environment[name]?.takeIf { it.isNotBlank() } ?: defaultValue
    private fun readInt(name: String, defaultValue: Int): Int = environment[name]?.toIntOrNull() ?: defaultValue
    private fun readLong(name: String, defaultValue: Long): Long = environment[name]?.toLongOrNull() ?: defaultValue
}

