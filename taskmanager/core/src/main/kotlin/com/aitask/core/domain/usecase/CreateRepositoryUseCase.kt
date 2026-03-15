package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.CreateRepositoryRequest
import com.aitask.core.domain.model.Repository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.validation.RepositoryValidator
import java.time.Instant
import java.util.UUID

class CreateRepositoryUseCase(
    private val repositoryRepository: RepositoryRepository,
    private val projectRepository: ProjectRepository,
    private val repositoryValidator: RepositoryValidator
) {
    suspend operator fun invoke(request: CreateRepositoryRequest): Result<Repository> {
        return try {
            // Validate repository
            val validation = repositoryValidator.validate(request)
            if (!validation.isValid) {
                return Result.failure(
                    ValidationException("Repository validation failed: ${validation.errors.joinToString { it.message }}")
                )
            }
            
            // Verify project exists
            val project = projectRepository.findById(request.projectId)
                ?: return Result.failure(ProjectNotFoundException("Project with id ${request.projectId} not found"))
            
            if (project.isArchived) {
                return Result.failure(ArchivedProjectException("Cannot add repository to archived project"))
            }
            
            // Check for duplicate repository name within the project
            val existingRepos = repositoryRepository.findByProject(request.projectId)
            val duplicate = existingRepos.find { it.name.equals(request.name, ignoreCase = true) }
            if (duplicate != null) {
                return Result.failure(
                    DuplicateRepositoryException("Repository with name '${request.name}' already exists in this project")
                )
            }
            
            // Check for duplicate clone URL within the project
            val duplicateUrl = existingRepos.find { it.cloneUrl == request.cloneUrl }
            if (duplicateUrl != null) {
                return Result.failure(
                    DuplicateRepositoryException("Repository with clone URL '${request.cloneUrl}' already exists in this project")
                )
            }
            
            // If this is being set as primary, ensure only one primary exists
            val isPrimary = if (request.isPrimary) {
                // Check if there's already a primary repository
                val existingPrimary = repositoryRepository.findPrimaryByProject(request.projectId)
                if (existingPrimary != null) {
                    return Result.failure(
                        PrimaryRepositoryException("Project already has a primary repository: ${existingPrimary.name}. Update the existing primary repository first.")
                    )
                }
                true
            } else {
                // If no repositories exist yet, this must be primary
                existingRepos.isEmpty()
            }
            
            // Create repository
            val now = Instant.now()
            val repository = Repository(
                id = UUID.randomUUID(),
                projectId = request.projectId,
                name = request.name,
                cloneUrl = request.cloneUrl,
                provider = request.provider,
                authType = request.authType,
                preferredIDEs = request.preferredIDEs,
                isPrimary = isPrimary,
                createdAt = now,
                updatedAt = now
            )
            
            val createdRepository = repositoryRepository.create(repository)
            Result.success(createdRepository)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DuplicateRepositoryException(message: String) : Exception(message)
class PrimaryRepositoryException(message: String) : Exception(message)

