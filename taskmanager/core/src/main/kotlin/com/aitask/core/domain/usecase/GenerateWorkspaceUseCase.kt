package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.BmadWorkspaceInjectionService
import com.aitask.core.domain.service.WorkspaceService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

class GenerateWorkspaceUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val repositoryRepository: RepositoryRepository,
    private val workspaceService: WorkspaceService,
    private val bmadWorkspaceInjectionService: BmadWorkspaceInjectionService,
    private val applyRulesToWorkspaceUseCase: ApplyRulesToWorkspaceUseCase,
    private val activityRepository: ActivityRepository
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
                logger.warn { "No repositories found for project ${project.id} (${project.name})" }
                return Result.failure(NoRepositoriesException("No repositories found for project. Add at least one repository to the project first."))
            }
            
            onProgress?.invoke("Creating workspace structure...")
            
            // Create workspace
            val workspaceResult = workspaceService.createWorkspace(task, project, repositories)
            if (workspaceResult.isFailure) {
                val err = workspaceResult.exceptionOrNull()!!
                logger.error(err) { "Workspace creation failed: ${err.rootCauseMessage()}" }
                recordActivity(
                    type = ActivityType.WORKSPACE_PREPARED,
                    task = task,
                    projectId = project.id,
                    status = ActivityStatus.FAILED,
                    description = "Workspace creation failed for task: ${task.title}",
                    metadata = mapOf("error" to (err.message ?: "Unknown"))
                )
                return Result.failure(err)
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
                val err = preparedWorkspaceResult.exceptionOrNull()!!
                logger.error(err) { "Workspace preparation (clone) failed: ${err.rootCauseMessage()}" }
                recordActivity(
                    type = ActivityType.GIT_PREPARED,
                    task = task,
                    projectId = project.id,
                    status = ActivityStatus.FAILED,
                    description = "Git clone failed for task: ${task.title}",
                    metadata = mapOf("error" to (err.message ?: "Unknown"))
                )
                return Result.failure(err)
            }
            
            val preparedWorkspace = preparedWorkspaceResult.getOrThrow()
            recordActivity(
                type = ActivityType.GIT_PREPARED,
                task = task,
                projectId = project.id,
                status = ActivityStatus.SUCCESS,
                description = "Cloned repositories for task: ${task.title}",
                metadata = mapOf("workspacePath" to preparedWorkspace.path)
            )

            var workspaceWithBmad = preparedWorkspace
            if (preparedWorkspace.isCompleted && task.effectiveMethodology(project.methodology) == Methodology.BMAD) {
                onProgress?.invoke("Injecting BMAD setup files...")
                val injectionResult = bmadWorkspaceInjectionService.injectIntoWorkspace(preparedWorkspace.path)
                if (injectionResult.isFailure) {
                    val err = injectionResult.exceptionOrNull()!!
                    logger.error(err) { "BMAD injection failed for workspace ${preparedWorkspace.path}: ${err.rootCauseMessage()}" }
                    recordActivity(
                        type = ActivityType.BMAD_INJECTION_FAILED,
                        task = task,
                        projectId = project.id,
                        status = ActivityStatus.FAILED,
                        description = "BMAD injection failed for task: ${task.title}",
                        metadata = mapOf(
                            "workspacePath" to preparedWorkspace.path,
                            "error" to (err.message ?: "Unknown")
                        )
                    )
                    onProgress?.invoke("BMAD injection failed: ${err.rootCauseMessage()}")
                    return Result.failure(err)
                }

                val injection = injectionResult.getOrThrow()
                workspaceWithBmad = preparedWorkspace.copy(
                    errorMessage = preparedWorkspace.errorMessage
                )
                recordActivity(
                    type = ActivityType.BMAD_INJECTION_APPLIED,
                    task = task,
                    projectId = project.id,
                    status = ActivityStatus.SUCCESS,
                    description = "BMAD injection applied for task: ${task.title}",
                    metadata = mapOf(
                        "workspacePath" to preparedWorkspace.path,
                        "sourcePath" to (injection.sourcePath ?: ""),
                        "copiedPaths" to injection.copiedPaths.joinToString(","),
                        "skippedPaths" to injection.skippedPaths.joinToString(","),
                        "overwriteExisting" to injection.overwriteExisting.toString()
                    )
                )
                val progressSummary = buildString {
                    append("BMAD setup ready")
                    if (injection.copiedPaths.isNotEmpty()) {
                        append(": copied ${injection.copiedPaths.joinToString(", ")}")
                    }
                    if (injection.skippedPaths.isNotEmpty()) {
                        append(" (skipped existing ${injection.skippedPaths.joinToString(", ")})")
                    }
                }
                onProgress?.invoke(progressSummary)
            }

            // Apply rules to workspace if preparation was successful
            var workspaceWithRules = workspaceWithBmad
            if (workspaceWithBmad.isCompleted) {
                onProgress?.invoke("Applying rules to workspace...")

                val applyRulesRequest = ApplyRulesRequest(
                    workspacePath = workspaceWithBmad.path,
                    projectId = project.id,
                    ideType = request.ideType,
                    selectedRepositories = request.selectedRepositories
                )

                val rulesResult = applyRulesToWorkspaceUseCase(applyRulesRequest)

                if (rulesResult.isSuccess) {
                    val appliedRules = rulesResult.getOrThrow()
                    workspaceWithRules = workspaceWithBmad.copy(
                        appliedRules = appliedRules.appliedRuleIds
                    )

                    // Report rule application results
                    onProgress?.invoke("Applied ${appliedRules.totalRulesApplied} rules, skipped ${appliedRules.totalRulesSkipped}")

                    if (appliedRules.skippedRules.isNotEmpty()) {
                        val skippedSummary = appliedRules.skippedRules
                            .groupBy { it.reason }
                            .map { (reason, rules) -> "${reason.name}: ${rules.size}" }
                            .joinToString(", ")
                        onProgress?.invoke("Skipped rules: $skippedSummary")
                    }
                } else {
                    // Rule application failed, but don't fail the entire workspace generation
                    val error = rulesResult.exceptionOrNull()
                    onProgress?.invoke("Warning: Failed to apply rules - ${error?.message}")
                }
            }

            // Update task with workspace path, branch name, and status only if workspace is completed
            if (workspaceWithRules.isCompleted) {
                // Get branch name from first repository (all should have the same branch name)
                val branchName = workspaceWithRules.repositories.firstOrNull()?.branchName

                val updatedTask = task.copy(
                    workspacePath = workspaceWithRules.path,
                    branchName = branchName,
                    status = TaskStatus.IN_PROGRESS
                )
                taskRepository.update(updatedTask)
                recordActivity(
                    type = ActivityType.WORKSPACE_PREPARED,
                    task = task,
                    projectId = project.id,
                    status = ActivityStatus.SUCCESS,
                    description = "Workspace prepared for task: ${task.title}",
                    metadata = mapOf("workspacePath" to workspaceWithRules.path)
                )
                onProgress?.invoke("Workspace generation complete!")
            } else if (workspaceWithRules.isFailed) {
                // Build detailed error message listing failed and successful repositories
                val failedRepos = workspaceWithRules.repositories.filter { it.cloneStatus == CloneStatus.FAILED }
                val successfulRepos = workspaceWithRules.repositories.filter { it.cloneStatus == CloneStatus.COMPLETED }

                val errorDetails = buildString {
                    append("Workspace generation partially failed. ")

                    if (failedRepos.isNotEmpty()) {
                        append("Failed repositories: ")
                        append(failedRepos.joinToString(", ") { repo ->
                            val repoEntity = repositories.find { it.id == repo.repositoryId }
                            "${repoEntity?.name ?: "Unknown"} (${repo.errorMessage ?: "Unknown error"})"
                        })
                        append(". ")
                    }

                    if (successfulRepos.isNotEmpty()) {
                        append("Successfully cloned: ")
                        append(successfulRepos.joinToString(", ") { repo ->
                            val repoEntity = repositories.find { it.id == repo.repositoryId }
                            repoEntity?.name ?: "Unknown"
                        })
                        append(". ")
                        append("Workspace path: ${workspaceWithRules.path}")
                    } else {
                        append("No repositories were successfully cloned.")
                    }
                }

                recordActivity(
                    type = ActivityType.WORKSPACE_PREPARED,
                    task = task,
                    projectId = project.id,
                    status = ActivityStatus.FAILED,
                    description = "Workspace preparation partially failed for task: ${task.title}",
                    metadata = mapOf("error" to errorDetails)
                )
                onProgress?.invoke(errorDetails)
                return Result.failure(WorkspaceGenerationException(errorDetails))
            }

            Result.success(workspaceWithRules)
        } catch (e: Exception) {
            val errorMsg = e.rootCauseMessage()
            logger.error(e) { "Workspace generation failed for task ${request.taskId}: $errorMsg" }
            val task = taskRepository.findById(request.taskId)
            if (task != null) {
                recordActivity(
                    type = ActivityType.WORKSPACE_PREPARED,
                    task = task,
                    projectId = request.projectId,
                    status = ActivityStatus.FAILED,
                    description = "Workspace generation failed: ${task.title}",
                    metadata = mapOf("error" to errorMsg)
                )
            }
            onProgress?.invoke("Workspace generation failed: $errorMsg")
            Result.failure(e)
        }
    }

    private suspend fun recordActivity(
        type: ActivityType,
        task: Task,
        projectId: UUID,
        status: ActivityStatus,
        description: String,
        metadata: Map<String, String>
    ) {
        activityRepository.create(
            Activity(
                id = UUID.randomUUID(),
                type = type,
                entityType = "task",
                entityId = task.id,
                description = description,
                metadata = metadata,
                status = status,
                createdAt = Instant.now(),
                projectId = projectId
            )
        )
    }
}

private fun Throwable.rootCauseMessage(): String {
    val rootCause = generateSequence(this) { it.cause }.last()
    return rootCause.message ?: this.message ?: this.javaClass.simpleName
}

class NoRepositoriesException(message: String) : Exception(message)
class WorkspaceGenerationException(message: String) : Exception(message)
