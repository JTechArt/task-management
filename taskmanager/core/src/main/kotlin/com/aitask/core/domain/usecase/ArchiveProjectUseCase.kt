package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Project
import com.aitask.core.domain.repository.ProjectRepository
import java.util.UUID

class ArchiveProjectUseCase(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(id: UUID): Result<Project> {
        return try {
            val existing = projectRepository.findById(id)
                ?: return Result.failure(ProjectNotFoundException("Project with id $id not found"))
            
            if (existing.isArchived) {
                return Result.failure(ArchivedProjectException("Project is already archived"))
            }
            
            val archived = projectRepository.archive(id)
                ?: return Result.failure(ProjectNotFoundException("Failed to archive project"))
            
            Result.success(archived)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

