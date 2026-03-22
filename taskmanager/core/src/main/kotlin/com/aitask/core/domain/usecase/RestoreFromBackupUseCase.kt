package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.DataExportBundle
import com.aitask.core.domain.model.ImportResult
import com.aitask.core.domain.repository.ActivityRepository
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

private const val REQUIRED_SCHEMA_VERSION = 1

/**
 * Validates backup integrity and restores from a backup file. AC2, AC4, AC5.
 * AC3 (warn about overwrite) is handled by the UI confirmation dialog.
 */
class RestoreFromBackupUseCase(
    private val importDataUseCase: ImportDataUseCase,
    private val activityRepository: ActivityRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Validates backup file integrity before applying. AC4.
     * Returns BackupValidation with summary of contents if valid.
     */
    fun validateIntegrity(content: String): Result<BackupValidation> {
        return try {
            val bundle = json.decodeFromString(DataExportBundle.serializer(), content)
            if (bundle.schemaVersion != REQUIRED_SCHEMA_VERSION) {
                return Result.failure(
                    IllegalArgumentException("Unsupported backup schema version: ${bundle.schemaVersion}. Required: $REQUIRED_SCHEMA_VERSION")
                )
            }
            Result.success(
                BackupValidation(
                    schemaVersion = bundle.schemaVersion,
                    projectCount = bundle.projects.size,
                    taskCount = bundle.tasks.size,
                    repositoryCount = bundle.repositories.size,
                    ruleCount = bundle.rules.size,
                    exportedAt = bundle.exportedAt
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid backup format: ${e.message}", e))
        }
    }

    suspend fun apply(content: String): Result<ImportResult> {
        val validation = validateIntegrity(content)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }
        return try {
            val importResult = importDataUseCase.apply(content)
            importResult.fold(
                onSuccess = { result ->
                    recordActivity(
                        status = ActivityStatus.SUCCESS,
                        description = "Restore completed: ${result.projectsCreated} projects, ${result.tasksCreated} tasks",
                        metadata = mapOf(
                            "projectsCreated" to result.projectsCreated.toString(),
                            "tasksCreated" to result.tasksCreated.toString(),
                            "reposCreated" to result.reposCreated.toString(),
                            "rulesCreated" to result.rulesCreated.toString()
                        )
                    )
                    Result.success(result)
                },
                onFailure = { e ->
                    recordActivity(
                        status = ActivityStatus.FAILED,
                        description = "Restore failed: ${e.message}",
                        metadata = mapOf("error" to (e.message ?: "Unknown"))
                    )
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            recordActivity(
                status = ActivityStatus.FAILED,
                description = "Restore failed: ${e.message}",
                metadata = mapOf("error" to (e.message ?: "Unknown"))
            )
            Result.failure(e)
        }
    }

    private suspend fun recordActivity(
        status: ActivityStatus,
        description: String,
        metadata: Map<String, String>
    ) {
        activityRepository.create(
            Activity(
                id = UUID.randomUUID(),
                type = ActivityType.RESTORE_COMPLETED,
                entityType = "restore",
                entityId = UUID.randomUUID(),
                description = description,
                metadata = metadata,
                status = status,
                createdAt = Instant.now(),
                projectId = null
            )
        )
    }
}

data class BackupValidation(
    val schemaVersion: Int,
    val projectCount: Int,
    val taskCount: Int,
    val repositoryCount: Int,
    val ruleCount: Int,
    val exportedAt: String
)
