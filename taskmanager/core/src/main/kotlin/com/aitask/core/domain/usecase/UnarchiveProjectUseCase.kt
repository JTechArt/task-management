package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Project
import com.aitask.core.domain.repository.ProjectRepository
import java.util.UUID

class UnarchiveProjectUseCase(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(id: UUID): Result<Project> {
        return try {
            val existing = projectRepository.findById(id)
                ?: return Result.failure(ProjectNotFoundException("Project with id $id not found"))
            if (!existing.isArchived) {
                return Result.failure(IllegalStateException("Project is not archived"))
            }
            val unarchived = projectRepository.unarchive(id)
                ?: return Result.failure(ProjectNotFoundException("Failed to unarchive project"))
            Result.success(unarchived)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
