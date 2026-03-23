package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.CreateProjectRequest
import com.aitask.core.domain.model.CreateRepositoryRequest
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Repository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.validation.ProjectValidator
import com.aitask.core.domain.validation.RepositoryValidator
import java.time.Instant
import java.util.UUID

class CreateProjectUseCase(
    private val projectRepository: ProjectRepository,
    private val repositoryRepository: RepositoryRepository,
    private val projectValidator: ProjectValidator,
    private val repositoryValidator: RepositoryValidator
) {
    suspend operator fun invoke(
        projectRequest: CreateProjectRequest,
        repositoryRequest: CreateRepositoryRequest
    ): Result<Pair<Project, Repository>> {
        return try {
            // Validate project
            val projectValidation = projectValidator.validate(projectRequest)
            if (!projectValidation.isValid) {
                return Result.failure(
                    ValidationException("Project validation failed: ${projectValidation.errors.joinToString { it.message }}")
                )
            }
            
            // Validate repository
            val repositoryValidation = repositoryValidator.validate(repositoryRequest)
            if (!repositoryValidation.isValid) {
                return Result.failure(
                    ValidationException("Repository validation failed: ${repositoryValidation.errors.joinToString { it.message }}")
                )
            }
            
            // Check for duplicate project name
            val existing = projectRepository.findByName(projectRequest.name)
            if (existing != null) {
                return Result.failure(
                    DuplicateProjectException("Project with name '${projectRequest.name}' already exists")
                )
            }
            
            // Create project
            val now = Instant.now()
            val projectId = UUID.randomUUID()
            val project = Project(
                id = projectId,
                name = projectRequest.name,
                description = projectRequest.description,
                workspacePath = projectRequest.workspacePath,
                branchTemplate = projectRequest.branchTemplate,
                methodology = projectRequest.methodology,
                bmadToolIds = if (projectRequest.methodology == com.aitask.core.domain.model.Methodology.BMAD &&
                    projectRequest.bmadToolIds.isEmpty()
                ) {
                    com.aitask.core.domain.model.BmadToolCatalog.defaultToolIds
                } else {
                    projectRequest.bmadToolIds
                },
                retentionPolicy = projectRequest.retentionPolicy,
                tags = projectRequest.tags,
                team = projectRequest.team,
                createdAt = now,
                updatedAt = now
            )
            
            val createdProject = projectRepository.create(project)
            
            // Create repository
            val repository = Repository(
                id = UUID.randomUUID(),
                projectId = projectId,
                name = repositoryRequest.name,
                cloneUrl = repositoryRequest.cloneUrl,
                provider = repositoryRequest.provider,
                authType = repositoryRequest.authType,
                preferredIDEs = repositoryRequest.preferredIDEs,
                isPrimary = true, // First repository is always primary
                createdAt = now,
                updatedAt = now
            )
            
            val createdRepository = repositoryRepository.create(repository)
            
            Result.success(createdProject to createdRepository)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ValidationException(message: String) : Exception(message)
class DuplicateProjectException(message: String) : Exception(message)
