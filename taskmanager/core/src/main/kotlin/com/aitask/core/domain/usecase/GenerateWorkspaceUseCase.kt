package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.CreateWorkspaceRequest
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.model.Workspace
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.WorkspaceService
import java.util.UUID

class GenerateWorkspaceUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val repositoryRepository: RepositoryRepository,
    private val workspaceService: WorkspaceService
) {
    suspend operator fun invoke(
        request: CreateWorkspaceRequest,
        onProgress: ((String) -> Unit)? = null
    ): Result<Workspace> {
        return try {
            onProgress?.invoke("Validating task and project...")
            
            // Validate task exists
            val task = taskRepository.findById(request.taskId)
                ?: return Result.failure(TaskNotFoundException("Task with id ${request.taskId} not found"))
            
            // Validate project exists
            val project = projectRepository.findById(request.projectId)
                ?: return Result.failure(ProjectNotFoundException("Project with id ${request.projectId} not found"))
            
            // Check if project is archived
            if (project.isArchived) {
                return Result.failure(ArchivedProjectException("Cannot generate workspace for archived project"))
            }
            
            // Check if task is archived
            if (task.isArchived) {
                return Result.failure(ArchivedTaskException("Cannot generate workspace for archived task"))
            }
            
            // Get repositories to clone
            val repositories = if (request.selectedRepositories.isEmpty()) {
                // Use primary repository if no specific repositories selected
                val primaryRepo = repositoryRepository.findPrimaryByProject(project.id)
                if (primaryRepo != null) {
                    listOf(primaryRepo)
                } else {
                    // Fall back to all repositories
                    repositoryRepository.findByProject(project.id)
                }
            } else {
                // Get selected repositories
                request.selectedRepositories.mapNotNull { repoId ->
                    repositoryRepository.findById(repoId)
                }
            }
            
            if (repositories.isEmpty()) {
                return Result.failure(NoRepositoriesException("No repositories found for project"))
            }
            
            onProgress?.invoke("Creating workspace structure...")
            
            // Create workspace
            val workspaceResult = workspaceService.createWorkspace(task, project, repositories)
            if (workspaceResult.isFailure) {
                return Result.failure(workspaceResult.exceptionOrNull()!!)
            }
            
            val workspace = workspaceResult.getOrThrow()
            
            // Prepare workspace (clone repositories)
            onProgress?.invoke("Cloning repositories...")
            val preparedWorkspaceResult = workspaceService.prepareWorkspace(
                workspace,
                repositories,
                onProgress
            )
            
            if (preparedWorkspaceResult.isFailure) {
                return Result.failure(preparedWorkspaceResult.exceptionOrNull()!!)
            }
            
            val preparedWorkspace = preparedWorkspaceResult.getOrThrow()

            // Update task with workspace path and status only if workspace is completed
            if (preparedWorkspace.isCompleted) {
                val updatedTask = task.copy(
                    workspacePath = preparedWorkspace.path,
                    status = TaskStatus.IN_PROGRESS
                )
                taskRepository.update(updatedTask)
                onProgress?.invoke("Workspace generation complete!")
            } else if (preparedWorkspace.isFailed) {
                onProgress?.invoke("Workspace generation failed: ${preparedWorkspace.errorMessage}")
                return Result.failure(WorkspaceGenerationException(
                    preparedWorkspace.errorMessage ?: "Workspace generation failed"
                ))
            }

            Result.success(preparedWorkspace)
        } catch (e: Exception) {
            onProgress?.invoke("Workspace generation failed: ${e.message}")
            Result.failure(e)
        }
    }
}

class NoRepositoriesException(message: String) : Exception(message)
class WorkspaceGenerationException(message: String) : Exception(message)

