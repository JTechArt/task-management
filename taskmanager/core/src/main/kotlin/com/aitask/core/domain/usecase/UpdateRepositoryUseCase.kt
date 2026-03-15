package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Repository
import com.aitask.core.domain.model.UpdateRepositoryRequest
import com.aitask.core.domain.repository.RepositoryRepository
import java.util.UUID

class UpdateRepositoryUseCase(
    private val repositoryRepository: RepositoryRepository
) {
    suspend operator fun invoke(id: UUID, request: UpdateRepositoryRequest): Result<Repository> {
        return try {
            val existing = repositoryRepository.findById(id)
                ?: return Result.failure(RepositoryNotFoundException("Repository with id $id not found"))
            
            // Check for duplicate name if name is being changed
            if (request.name != null && request.name != existing.name) {
                val projectRepos = repositoryRepository.findByProject(existing.projectId)
                val duplicate = projectRepos.find { 
                    it.name.equals(request.name, ignoreCase = true) && it.id != id 
                }
                if (duplicate != null) {
                    return Result.failure(
                        DuplicateRepositoryException("Repository with name '${request.name}' already exists in this project")
                    )
                }
            }
            
            // Check for duplicate clone URL if being changed
            if (request.cloneUrl != null && request.cloneUrl != existing.cloneUrl) {
                val projectRepos = repositoryRepository.findByProject(existing.projectId)
                val duplicate = projectRepos.find { 
                    it.cloneUrl == request.cloneUrl && it.id != id 
                }
                if (duplicate != null) {
                    return Result.failure(
                        DuplicateRepositoryException("Repository with clone URL '${request.cloneUrl}' already exists in this project")
                    )
                }
            }
            
            // Handle primary repository changes
            if (request.isPrimary != null && request.isPrimary != existing.isPrimary) {
                if (request.isPrimary) {
                    // Setting this as primary - check if another primary exists
                    val existingPrimary = repositoryRepository.findPrimaryByProject(existing.projectId)
                    if (existingPrimary != null && existingPrimary.id != id) {
                        return Result.failure(
                            PrimaryRepositoryException("Project already has a primary repository: ${existingPrimary.name}. Unset it first before setting a new primary.")
                        )
                    }
                } else {
                    // Unsetting primary - ensure at least one repository remains
                    val projectRepos = repositoryRepository.findByProject(existing.projectId)
                    if (projectRepos.size == 1) {
                        return Result.failure(
                            PrimaryRepositoryException("Cannot unset primary flag on the only repository in the project")
                        )
                    }
                }
            }
            
            val updated = existing.update(
                name = request.name,
                cloneUrl = request.cloneUrl,
                provider = request.provider,
                authType = request.authType,
                preferredIDEs = request.preferredIDEs,
                isPrimary = request.isPrimary
            )
            
            val savedRepository = repositoryRepository.update(updated)
            Result.success(savedRepository)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RepositoryNotFoundException(message: String) : Exception(message)

