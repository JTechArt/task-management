package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

enum class AgentScope {
    GLOBAL,
    PROJECT
}

enum class AgentTrigger {
    MANUAL,
    TASK_OPENED,
    TASK_UPDATED
}

data class AgentDefinitionRequest(
    val id: UUID? = null,
    val name: String,
    val description: String? = null,
    val promptTemplate: String,
    val llmConfigurationId: UUID? = null,
    val scope: AgentScope = AgentScope.GLOBAL,
    val projectId: UUID? = null,
    val trigger: AgentTrigger = AgentTrigger.MANUAL,
    val isEnabled: Boolean = true
) {
    init {
        require(name.isNotBlank()) { "Agent name must not be blank" }
        require(promptTemplate.isNotBlank()) { "Agent prompt template must not be blank" }
        if (scope == AgentScope.PROJECT) {
            require(projectId != null) { "Project-scoped agents must be assigned to a project" }
        }
    }
}

data class AgentDefinition(
    val id: UUID,
    val name: String,
    val description: String?,
    val promptTemplate: String,
    val llmConfigurationId: UUID?,
    val scope: AgentScope,
    val projectId: UUID?,
    val trigger: AgentTrigger,
    val isEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(name.isNotBlank()) { "Agent name must not be blank" }
        require(promptTemplate.isNotBlank()) { "Agent prompt template must not be blank" }
        if (scope == AgentScope.PROJECT) {
            require(projectId != null) { "Project-scoped agents must be assigned to a project" }
        }
    }
}

data class AgentExecutionResult(
    val generatedText: String,
    val configurationName: String,
    val modelIdentifier: String,
    val endpointUrl: String
)
