package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.repository.ActivityRepository
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Creates a backup of application data and records the action in activity history. AC1, AC5.
 * Uses ExportDataUseCase; credentials excluded per Export Security.
 */
class CreateBackupUseCase(
    private val exportDataUseCase: ExportDataUseCase,
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(
        targetFile: File,
        includeArchived: Boolean = false
    ): Result<File> {
        return try {
            val exportResult = exportDataUseCase(includeArchived)
            exportResult.fold(
                onSuccess = { json ->
                    targetFile.writeText(json)
                    recordActivity(
                        status = ActivityStatus.SUCCESS,
                        description = "Backup created: ${targetFile.name}",
                        metadata = mapOf(
                            "path" to targetFile.absolutePath,
                            "projects" to countFromJson(json, "projects"),
                            "tasks" to countFromJson(json, "tasks")
                        )
                    )
                    Result.success(targetFile)
                },
                onFailure = { e ->
                    recordActivity(
                        status = ActivityStatus.FAILED,
                        description = "Backup failed: ${e.message}",
                        metadata = mapOf("error" to (e.message ?: "Unknown"))
                    )
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            recordActivity(
                status = ActivityStatus.FAILED,
                description = "Backup failed: ${e.message}",
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
                type = ActivityType.BACKUP_CREATED,
                entityType = "backup",
                entityId = UUID.randomUUID(),
                description = description,
                metadata = metadata,
                status = status,
                createdAt = Instant.now(),
                projectId = null
            )
        )
    }

    private fun countFromJson(json: String, key: String): String {
        return try {
            val regex = """"$key"\s*:\s*\[([^\]]*)\]""".toRegex()
            regex.find(json)?.groupValues?.get(1)?.let { content ->
                if (content.isBlank()) "0" else content.split("\"id\"").size.minus(1).coerceAtLeast(0).toString()
            } ?: "?"
        } catch (_: Exception) {
            "?"
        }
    }
}
