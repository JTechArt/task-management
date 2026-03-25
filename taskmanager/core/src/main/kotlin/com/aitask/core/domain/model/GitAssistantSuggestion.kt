package com.aitask.core.domain.model

import java.util.UUID

enum class GitAssistantSuggestionMode {
    COMMIT_MESSAGE,
    PR_DESCRIPTION,
    COMMENT_DRAFT
}

data class GitAssistantSuggestionRequest(
    val projectId: UUID,
    val projectName: String? = null,
    val taskId: UUID,
    val taskTitle: String,
    val taskDescription: String?,
    val branchName: String?,
    val repositoryNames: List<String> = emptyList(),
    val stagedChangesSummary: String? = null,
    val mode: GitAssistantSuggestionMode
) {
    init {
        require(taskTitle.isNotBlank()) { "Task title must not be blank" }
    }
}

data class GitAssistantSuggestionResult(
    val generatedText: String,
    val configurationName: String,
    val modelIdentifier: String,
    val endpointUrl: String
)
