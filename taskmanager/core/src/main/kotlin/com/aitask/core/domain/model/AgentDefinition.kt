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

/**
 * AI backends an agent may be associated with. Execution currently requires [LOCAL_LLM] for the built-in runner.
 */
enum class AgentToolKind {
    LOCAL_LLM,
    CODEX,
    CLAUDE
}

object AgentToolKindCodec {
    fun encode(tools: Set<AgentToolKind>): String =
        tools.sortedBy { it.ordinal }.joinToString(",") { it.name }
    fun decode(raw: String?): Set<AgentToolKind> {
        if (raw.isNullOrBlank()) {
            return setOf(AgentToolKind.LOCAL_LLM)
        }
        val parsed: Set<AgentToolKind> = raw.split(",")
            .mapNotNull { token -> runCatching { AgentToolKind.valueOf(token.trim()) }.getOrNull() }
            .toSet()
        return parsed.ifEmpty { setOf(AgentToolKind.LOCAL_LLM) }
    }
}

/**
 * Automation workflow backed by a persisted [AgentDefinition] (prompt, triggers, tools, scope).
 */
data class AutomationWorkflow(
    val definition: AgentDefinition
) {
    val id: UUID get() = definition.id
    val name: String get() = definition.name
}

fun AgentDefinition.toAutomationWorkflow(): AutomationWorkflow = AutomationWorkflow(definition = this)

data class AgentDefinitionRequest(
    val id: UUID? = null,
    val name: String,
    val description: String? = null,
    val promptTemplate: String,
    val llmConfigurationId: UUID? = null,
    val associatedTools: Set<AgentToolKind> = setOf(AgentToolKind.LOCAL_LLM),
    /** When set, the agent applies only to tasks of this type; null means all task types. */
    val taskTypeFilter: TaskType? = null,
    val scope: AgentScope = AgentScope.GLOBAL,
    val projectId: UUID? = null,
    val trigger: AgentTrigger = AgentTrigger.MANUAL,
    val isEnabled: Boolean = true
) {
    init {
        require(name.isNotBlank()) { "Agent name must not be blank" }
        require(promptTemplate.isNotBlank()) { "Agent prompt template must not be blank" }
        require(associatedTools.isNotEmpty()) { "Select at least one AI tool association" }
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
    val associatedTools: Set<AgentToolKind>,
    /** When non-null, the agent is associated with this task type only; null means all task types. */
    val taskTypeFilter: TaskType?,
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
        require(associatedTools.isNotEmpty()) { "Agent must reference at least one AI tool" }
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
