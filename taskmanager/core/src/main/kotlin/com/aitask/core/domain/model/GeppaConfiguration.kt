package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

data class GeppaConfigurationRequest(
    val id: UUID? = null,
    val isEnabled: Boolean,
    val endpointUrl: String
) {
    init {
        if (isEnabled) {
            require(endpointUrl.isNotBlank()) { "GEPPA endpoint URL must not be blank when GEPPA is enabled" }
        }
    }
}

data class GeppaConfiguration(
    val id: UUID,
    val isEnabled: Boolean,
    val endpointUrl: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class GeppaConnectionTestResult(
    val probeUrl: String,
    val statusCode: Int,
    val latencyMillis: Long
)
