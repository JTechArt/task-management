package com.aitask.core.domain.model

/**
 * Health state for a monitored component
 */
enum class HealthState {
    HEALTHY,
    DEGRADED,
    FAILED,
    UNKNOWN
}

/**
 * Result of a health check for a single component
 */
data class ComponentHealth(
    val id: String,
    val label: String,
    val state: HealthState,
    val message: String,
    val detail: String? = null
)

/**
 * Repository-specific health for Git validation
 */
data class RepositoryHealth(
    val repositoryId: java.util.UUID,
    val repositoryName: String,
    val projectName: String,
    val cloneUrl: String,
    val state: HealthState,
    val message: String
)

/**
 * Aggregate health status for the Integrations view
 */
data class HealthStatus(
    val database: ComponentHealth,
    val repositories: ComponentHealth,
    val repositoryDetails: List<RepositoryHealth> = emptyList(),
    val ideDiscovery: ComponentHealth,
    val lastRefreshedAt: java.time.Instant? = null
)
