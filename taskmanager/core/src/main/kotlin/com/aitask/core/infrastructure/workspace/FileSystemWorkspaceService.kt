package com.aitask.core.infrastructure.workspace

import com.aitask.core.domain.model.*
import com.aitask.core.domain.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
class FileSystemWorkspaceService(
    private val gitService: GitService
) : WorkspaceService {
    
    override suspend fun createWorkspace(
        task: Task,
        project: Project,
        selectedRepositories: List<Repository>
    ): Result<Workspace> = withContext(Dispatchers.IO) {
        try {
            val workspacePath = getWorkspacePath(task, project)
            
            // Create workspace directory structure
            val workspaceDir = File(workspacePath)
            if (!workspaceDir.exists()) {
                workspaceDir.mkdirs()
            }
            
            // Create workspace repositories list
            val workspaceRepos = selectedRepositories.map { repo ->
                WorkspaceRepository(
                    repositoryId = repo.id,
                    localPath = "./${sanitizeName(repo.name)}",
                    branchName = null,
                    cloneStatus = CloneStatus.PENDING
                )
            }
            
            val workspace = Workspace(
                taskId = task.id,
                projectId = project.id,
                path = workspacePath,
                repositories = workspaceRepos,
                appliedRules = emptyList(),
                status = WorkspaceStatus.PENDING,
                createdAt = Instant.now()
            )
            
            Result.success(workspace)
        } catch (e: Exception) {
            Result.failure(WorkspaceCreationException("Failed to create workspace: ${e.message}", e))
        }
    }
    
    override suspend fun prepareWorkspace(
        workspace: Workspace,
        repositories: List<Repository>,
        onProgress: ((String) -> Unit)?
    ): Result<Workspace> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("Preparing workspace...")
            
            val updatedRepos = mutableListOf<WorkspaceRepository>()
            
            for (workspaceRepo in workspace.repositories) {
                val repository = repositories.find { it.id == workspaceRepo.repositoryId }
                    ?: continue
                
                onProgress?.invoke("Cloning ${repository.name}...")
                
                val repoPath = File(workspace.path, workspaceRepo.localPath).absolutePath
                
                // Clone repository
                val authConfig = GitAuthConfig(
                    authType = repository.authType,
                    username = "git",
                    password = null,
                    token = null
                )
                
                val cloneResult = gitService.cloneRepository(
                    url = repository.cloneUrl,
                    destination = repoPath,
                    authConfig = authConfig,
                    shallow = true,
                    onProgress = { progress ->
                        onProgress?.invoke("${repository.name}: ${progress.message}")
                    }
                )
                
                if (cloneResult.isSuccess) {
                    updatedRepos.add(
                        workspaceRepo.copy(
                            cloneStatus = CloneStatus.COMPLETED
                        )
                    )
                    onProgress?.invoke("${repository.name}: Cloned successfully")
                } else {
                    updatedRepos.add(
                        workspaceRepo.copy(
                            cloneStatus = CloneStatus.FAILED,
                            errorMessage = cloneResult.exceptionOrNull()?.message
                        )
                    )
                    onProgress?.invoke("${repository.name}: Clone failed")
                }
            }
            
            val allSuccessful = updatedRepos.all { it.cloneStatus == CloneStatus.COMPLETED }
            val updatedWorkspace = workspace.copy(
                repositories = updatedRepos,
                status = if (allSuccessful) WorkspaceStatus.COMPLETED else WorkspaceStatus.FAILED,
                errorMessage = if (!allSuccessful) "Some repositories failed to clone" else null
            )
            
            // Save workspace metadata
            saveWorkspaceMetadata(updatedWorkspace)
            
            onProgress?.invoke("Workspace preparation complete")
            
            Result.success(updatedWorkspace)
        } catch (e: Exception) {
            Result.failure(WorkspacePreparationException("Failed to prepare workspace: ${e.message}", e))
        }
    }
    
    override suspend fun cleanupWorkspace(
        workspacePath: String,
        retentionPolicy: RetentionPolicy
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            when (retentionPolicy) {
                RetentionPolicy.KEEP_ALL -> {
                    // Do nothing
                }
                RetentionPolicy.DELETE_ON_COMPLETION, RetentionPolicy.DELETE_AFTER_DAYS -> {
                    val path = Paths.get(workspacePath)
                    if (Files.exists(path)) {
                        path.deleteRecursively()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(WorkspaceCleanupException("Failed to cleanup workspace: ${e.message}", e))
        }
    }
    
    override suspend fun getWorkspacePath(task: Task, project: Project): String {
        val projectName = sanitizeName(project.name)
        val taskId = task.id.toString().take(8)
        return "${project.workspacePath}/$projectName/task-$taskId"
    }
    
    private fun sanitizeName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9\\-_]"), "-").lowercase()
    }
    
    private suspend fun saveWorkspaceMetadata(workspace: Workspace) = withContext(Dispatchers.IO) {
        try {
            val metadata = WorkspaceMetadata(
                taskId = workspace.taskId,
                projectId = workspace.projectId,
                createdAt = workspace.createdAt,
                repositories = workspace.repositories.map { repo ->
                    WorkspaceRepositoryMetadata(
                        repositoryId = repo.repositoryId,
                        localPath = repo.localPath,
                        branchName = repo.branchName,
                        cloneStatus = repo.cloneStatus.name
                    )
                },
                appliedRules = workspace.appliedRules
            )

            val metadataFile = File(workspace.path, "workspace-metadata.json")
            val json = buildString {
                appendLine("{")
                appendLine("  \"taskId\": \"${metadata.taskId}\",")
                appendLine("  \"projectId\": \"${metadata.projectId}\",")
                appendLine("  \"createdAt\": \"${metadata.createdAt}\",")
                appendLine("  \"repositories\": [")
                metadata.repositories.forEachIndexed { index, repo ->
                    appendLine("    {")
                    appendLine("      \"repositoryId\": \"${repo.repositoryId}\",")
                    appendLine("      \"localPath\": \"${repo.localPath}\",")
                    appendLine("      \"branchName\": ${if (repo.branchName != null) "\"${repo.branchName}\"" else "null"},")
                    appendLine("      \"cloneStatus\": \"${repo.cloneStatus}\"")
                    append("    }")
                    if (index < metadata.repositories.size - 1) appendLine(",")
                    else appendLine()
                }
                appendLine("  ],")
                appendLine("  \"appliedRules\": [")
                metadata.appliedRules.forEachIndexed { index, ruleId ->
                    append("    \"$ruleId\"")
                    if (index < metadata.appliedRules.size - 1) appendLine(",")
                    else appendLine()
                }
                appendLine("  ]")
                append("}")
            }

            metadataFile.writeText(json)
        } catch (e: Exception) {
            // Log error but don't fail workspace creation
            println("Warning: Failed to save workspace metadata: ${e.message}")
        }
    }
}

class WorkspaceCreationException(message: String, cause: Throwable? = null) : Exception(message, cause)
class WorkspacePreparationException(message: String, cause: Throwable? = null) : Exception(message, cause)
class WorkspaceCleanupException(message: String, cause: Throwable? = null) : Exception(message, cause)

