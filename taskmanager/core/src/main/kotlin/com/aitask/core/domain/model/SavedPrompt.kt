package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

data class SavedPromptRequest(
    val id: UUID? = null,
    val name: String,
    val content: String,
    val category: String? = null,
    val scope: AgentScope = AgentScope.GLOBAL,
    val projectId: UUID? = null
) {
    init {
        require(name.isNotBlank()) { "Prompt name must not be blank" }
        require(content.isNotBlank()) { "Prompt content must not be blank" }
        if (scope == AgentScope.PROJECT) {
            require(projectId != null) { "Project-scoped prompts must be assigned to a project" }
        }
    }
}

data class SavedPrompt(
    val id: UUID,
    val name: String,
    val content: String,
    val category: String?,
    val scope: AgentScope,
    val projectId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant
)
