package com.aitask.core.domain.service

import com.aitask.core.domain.model.HealthStatus

/**
 * Service for checking health of database, repositories, and integrations
 */
interface HealthCheckService {
    /**
     * Run all health checks and return aggregate status
     */
    suspend fun runHealthChecks(): HealthStatus
}
