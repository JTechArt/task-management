package com.aitask.core.domain.model

import java.util.UUID

/**
 * Represents an installed IDE on the system
 */
data class InstalledIDE(
    val type: IDEType,
    val version: String,
    val executablePath: String
)

/**
 * Context information about a task for IDE integration
 */
data class TaskContext(
    val taskId: UUID,
    val title: String,
    val description: String?,
    val projectName: String,
    val branchName: String?,
    val activeBmadTools: List<String> = emptyList(),
    val repositoryPath: String? = null
)

/**
 * Exception thrown when an IDE is not found
 */
class IDENotFoundException(ideType: IDEType) : 
    Exception("IDE not found: $ideType")

/**
 * Exception thrown when IDE launch fails
 */
class IDELaunchException(message: String, cause: Throwable? = null) : 
    Exception(message, cause)
