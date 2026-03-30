package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

enum class ClaudeIntegrationMode {
    CLI,
    API
}

data class ClaudeConfigurationRequest(
    val id: UUID? = null,
    val isEnabled: Boolean,
    val integrationMode: ClaudeIntegrationMode,
    val cliPath: String,
    val apiBaseUrl: String?,
    val apiKey: String? = null
)

data class ClaudeConfiguration(
    val id: UUID,
    val isEnabled: Boolean,
    val integrationMode: ClaudeIntegrationMode,
    val cliPath: String,
    val apiBaseUrl: String?,
    val hasApiKey: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
