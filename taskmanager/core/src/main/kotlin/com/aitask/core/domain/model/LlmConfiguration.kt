package com.aitask.core.domain.model
import java.time.Instant
import java.util.UUID

data class LlmConfigurationRequest(
    val id: UUID? = null,
    val name: String,
    val endpointUrl: String,
    val modelIdentifier: String,
    val apiKey: String? = null,
    val isDefault: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "LLM configuration name must not be blank" }
        require(endpointUrl.isNotBlank()) { "LLM endpoint URL must not be blank" }
        require(modelIdentifier.isNotBlank()) { "LLM model identifier must not be blank" }
    }
}

data class LlmConfiguration(
    val id: UUID,
    val name: String,
    val endpointUrl: String,
    val modelIdentifier: String,
    val hasApiKey: Boolean,
    val isDefault: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(name.isNotBlank()) { "LLM configuration name must not be blank" }
        require(endpointUrl.isNotBlank()) { "LLM endpoint URL must not be blank" }
        require(modelIdentifier.isNotBlank()) { "LLM model identifier must not be blank" }
    }
}

data class LlmConnectionTestResult(
    val probeUrl: String,
    val statusCode: Int,
    val latencyMillis: Long
)
