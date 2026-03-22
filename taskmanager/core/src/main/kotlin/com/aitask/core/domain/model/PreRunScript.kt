package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

enum class PreRunScriptType {
    INLINE_COMMAND,
    SCRIPT_PATH,
    NODE_VERSION,
    JAVA_VERSION,
    PYTHON_VERSION,
    ENVIRONMENT_VARIABLE,
    DEPENDENCY_PRESENT
}

data class PreRunScript(
    val id: UUID,
    val projectId: UUID,
    val repositoryId: UUID? = null,
    val name: String,
    val type: PreRunScriptType,
    val scriptPath: String? = null,
    val inlineScript: String? = null,
    val requiredValue: String? = null,
    val executionOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val isRepositoryScoped: Boolean
        get() = repositoryId != null

    fun update(
        name: String? = null,
        type: PreRunScriptType? = null,
        scriptPath: String? = null,
        inlineScript: String? = null,
        requiredValue: String? = null,
        executionOrder: Int? = null,
        repositoryId: UUID? = this.repositoryId
    ): PreRunScript = copy(
        name = name ?: this.name,
        type = type ?: this.type,
        scriptPath = scriptPath,
        inlineScript = inlineScript,
        requiredValue = requiredValue,
        executionOrder = executionOrder ?: this.executionOrder,
        repositoryId = repositoryId,
        updatedAt = Instant.now()
    )
}

data class PreRunExecutionResult(
    val script: PreRunScript,
    val succeeded: Boolean,
    val exitCode: Int,
    val stdout: String = "",
    val stderr: String = "",
    val resolvedCommand: String,
    val executedAt: Instant = Instant.now()
)

class PreRunScriptExecutionException(
    val result: PreRunExecutionResult,
    message: String = buildString {
        append("Pre-run script failed: ")
        append(result.script.name)
        if (result.stderr.isNotBlank()) {
            append(". ")
            append(result.stderr.trim())
        } else if (result.stdout.isNotBlank()) {
            append(". ")
            append(result.stdout.trim())
        }
    }
) : Exception(message)
