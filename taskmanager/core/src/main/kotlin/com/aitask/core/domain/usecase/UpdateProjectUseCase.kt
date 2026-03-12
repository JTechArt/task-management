package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.UpdateProjectRequest
import com.aitask.core.domain.repository.ProjectRepository
import java.util.UUID

class UpdateProjectUseCase(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(id: UUID, request: UpdateProjectRequest): Result<Project> {
        return try {
            val existing = projectRepository.findById(id)
                ?: return Result.failure(ProjectNotFoundException("Project with id $id not found"))
            
            if (existing.isArchived) {
                return Result.failure(ArchivedProjectException("Cannot update archived project"))
            }
            
            // Check for duplicate name if name is being changed
            if (request.name != null && request.name != existing.name) {
                val duplicate = projectRepository.findByName(request.name)
                if (duplicate != null && duplicate.id != id) {
                    return Result.failure(
                        DuplicateProjectException("Project with name '${request.name}' already exists")
                    )
                }
            }
            
            val updated = existing.update(
                name = request.name,
                description = request.description,
                workspacePath = request.workspacePath,
                branchTemplate = request.branchTemplate,
                tags = request.tags,
                team = request.team
            )
            
            val savedProject = projectRepository.update(updated)
            Result.success(savedProject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ProjectNotFoundException(message: String) : Exception(message)
class ArchivedProjectException(message: String) : Exception(message)

