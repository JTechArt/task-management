package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

data class CodexConfigurationRequest(
    val id: UUID? = null,
    val isEnabled: Boolean,
    val cliPath: String,
    val apiKey: String? = null
)

data class CodexConfiguration(
    val id: UUID,
    val isEnabled: Boolean,
    val cliPath: String,
    val hasApiKey: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
