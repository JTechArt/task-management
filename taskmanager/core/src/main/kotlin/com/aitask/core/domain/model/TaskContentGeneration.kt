package com.aitask.core.domain.model

enum class TaskContentGenerationMode {
    DESCRIPTION,
    SUMMARY
}

data class TaskContentGenerationRequest(
    val projectId: java.util.UUID,
    val projectName: String? = null,
    val title: String,
    val currentDescription: String?,
    val taskType: TaskType,
    val mode: TaskContentGenerationMode
) {
    init {
        require(title.isNotBlank()) { "Task title must not be blank" }
    }
}

data class TaskContentGenerationResult(
    val generatedText: String,
    val configurationName: String,
    val modelIdentifier: String,
    val endpointUrl: String
)
