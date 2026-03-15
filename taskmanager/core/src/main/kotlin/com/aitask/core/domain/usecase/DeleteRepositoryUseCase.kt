package com.aitask.core.domain.usecase

import com.aitask.core.domain.repository.RepositoryRepository
import java.util.UUID

class DeleteRepositoryUseCase(
    private val repositoryRepository: RepositoryRepository
) {
    suspend operator fun invoke(id: UUID): Result<Unit> {
        return try {
            val existing = repositoryRepository.findById(id)
                ?: return Result.failure(RepositoryNotFoundException("Repository with id $id not found"))
            
            // Check if this is the only repository in the project
            val projectRepos = repositoryRepository.findByProject(existing.projectId)
            if (projectRepos.size == 1) {
                return Result.failure(
                    CannotDeleteRepositoryException("Cannot delete the only repository in the project. Delete the project instead.")
                )
            }
            
            // If this is the primary repository, prevent deletion
            if (existing.isPrimary) {
                return Result.failure(
                    CannotDeleteRepositoryException("Cannot delete the primary repository. Set another repository as primary first.")
                )
            }
            
            repositoryRepository.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CannotDeleteRepositoryException(message: String) : Exception(message)

