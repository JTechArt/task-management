package com.aitask.core.infrastructure.git

import com.aitask.core.domain.model.AuthType
import com.aitask.core.domain.service.CloneProgress
import com.aitask.core.domain.service.GitAuthConfig
import com.aitask.core.domain.service.GitService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.*
import java.io.File

class JGitService : GitService {
    
    override suspend fun cloneRepository(
        url: String,
        destination: String,
        authConfig: GitAuthConfig,
        shallow: Boolean,
        onProgress: ((CloneProgress) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val destFile = File(destination)
            
            // Ensure parent directory exists
            destFile.parentFile?.mkdirs()
            
            val cloneCommand = Git.cloneRepository()
                .setURI(url)
                .setDirectory(destFile)
                .setCloneAllBranches(!shallow)
            
            if (shallow) {
                cloneCommand.setDepth(1)
            }
            
            // Set up authentication
            when (authConfig.authType) {
                AuthType.HTTPS, AuthType.TOKEN -> {
                    val credentialsProvider = UsernamePasswordCredentialsProvider(
                        authConfig.username ?: "git",
                        authConfig.password ?: authConfig.token ?: ""
                    )
                    cloneCommand.setCredentialsProvider(credentialsProvider)
                }
                AuthType.SSH -> {
                    // SSH support requires additional configuration
                    // For now, we'll use a simple approach
                    // In production, this would use SSH keys from the system
                }
            }
            
            // Set up progress monitoring
            if (onProgress != null) {
                cloneCommand.setProgressMonitor(object : ProgressMonitor {
                    private var currentTask = ""
                    private var totalWork = 0
                    private var completed = 0

                    override fun start(totalTasks: Int) {
                        onProgress(CloneProgress("Starting", 0, "Initializing clone"))
                    }

                    override fun beginTask(title: String, totalWork: Int) {
                        currentTask = title
                        this.totalWork = totalWork
                        this.completed = 0
                        onProgress(CloneProgress(title, 0, title))
                    }

                    override fun update(completed: Int) {
                        this.completed += completed
                        val percent = if (totalWork > 0) {
                            (this.completed * 100 / totalWork).coerceIn(0, 100)
                        } else 0
                        onProgress(CloneProgress(currentTask, percent, "$currentTask: $percent%"))
                    }

                    override fun endTask() {
                        onProgress(CloneProgress(currentTask, 100, "$currentTask: Complete"))
                    }

                    override fun isCancelled(): Boolean = false

                    override fun showDuration(enabled: Boolean) {
                        // Not used
                    }
                })
            }
            
            cloneCommand.call().use { git ->
                // Clone successful
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(GitCloneException("Failed to clone repository: ${e.message}", e))
        }
    }
    
    override suspend fun createBranch(
        repositoryPath: String,
        branchName: String,
        checkout: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Git.open(File(repositoryPath)).use { git ->
                git.branchCreate()
                    .setName(branchName)
                    .call()
                
                if (checkout) {
                    git.checkout()
                        .setName(branchName)
                        .call()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(GitBranchException("Failed to create branch: ${e.message}", e))
        }
    }
    
    override suspend fun validateRepository(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val gitDir = File(path, ".git")
            Result.success(gitDir.exists() && gitDir.isDirectory)
        } catch (e: Exception) {
            Result.success(false)
        }
    }
    
}

class GitCloneException(message: String, cause: Throwable? = null) : Exception(message, cause)
class GitBranchException(message: String, cause: Throwable? = null) : Exception(message, cause)

