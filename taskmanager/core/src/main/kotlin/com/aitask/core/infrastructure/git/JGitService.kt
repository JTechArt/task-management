package com.aitask.core.infrastructure.git

import com.aitask.core.domain.model.AuthType
import com.aitask.core.domain.service.CloneProgress
import com.aitask.core.domain.service.GitAuthConfig
import com.aitask.core.domain.service.GitService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder
import java.io.File

class JGitService : GitService {
    private val logger = KotlinLogging.logger {}
    
    override suspend fun cloneRepository(
        url: String,
        destination: String,
        authConfig: GitAuthConfig,
        shallow: Boolean,
        onProgress: ((CloneProgress) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val destFile = File(destination)
            logger.info { "Starting clone for $url into $destination with authType=${authConfig.authType}" }
            
            // Ensure parent directory exists
            destFile.parentFile?.mkdirs()
            
            val cloneCommand = Git.cloneRepository()
                .setURI(url)
                .setDirectory(destFile)
                .setCloneAllBranches(!shallow)
            
            if (shallow) {
                cloneCommand.setDepth(1)
            }

            configureTransportAuthentication(cloneCommand, authConfig)
            
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

            logger.info { "Clone completed for $url into $destination" }
            
            Result.success(Unit)
        } catch (e: Exception) {
            val detailedMessage = e.rootCauseMessage()
            logger.error(e) { "Clone failed for $url into $destination: $detailedMessage" }
            Result.failure(GitCloneException("Failed to clone repository: $detailedMessage", e))
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
            logger.error(e) { "Branch creation failed in $repositoryPath for $branchName: ${e.rootCauseMessage()}" }
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

    override suspend fun validateRemoteRepository(
        url: String,
        authConfig: GitAuthConfig
    ): Result<com.aitask.core.domain.service.RepositoryInfo> = withContext(Dispatchers.IO) {
        try {
            // Use ls-remote to validate repository access without cloning
            val lsRemoteCommand = Git.lsRemoteRepository()
                .setRemote(url)
                .setHeads(true)

            configureTransportAuthentication(lsRemoteCommand, authConfig)

            // Execute ls-remote
            val refs = lsRemoteCommand.call()

            // Find default branch (usually main or master)
            val defaultBranch = refs.firstOrNull { ref ->
                ref.name == "refs/heads/main" || ref.name == "refs/heads/master"
            }?.name?.removePrefix("refs/heads/")

            Result.success(
                com.aitask.core.domain.service.RepositoryInfo(
                    url = url,
                    isAccessible = true,
                    defaultBranch = defaultBranch ?: "main"
                )
            )
        } catch (e: Exception) {
            Result.success(
                com.aitask.core.domain.service.RepositoryInfo(
                    url = url,
                    isAccessible = false,
                    errorMessage = e.message ?: "Failed to access repository"
                )
            )
        }
    }

    override suspend fun getStagedChangesSummary(repositoryPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Git.open(File(repositoryPath)).use { git ->
                val status = git.status().call()
                val stagedFiles = buildList {
                    addAll(status.added)
                    addAll(status.changed)
                    addAll(status.removed)
                }.distinct().sorted()

                if (stagedFiles.isEmpty()) {
                    return@withContext Result.success("No staged changes detected.")
                }

                val diffSummary = buildString {
                    appendLine("Staged files:")
                    stagedFiles.forEach { file ->
                        appendLine("- $file")
                    }

                    val entries = runCatching { git.diff().setCached(true).call() }.getOrDefault(emptyList())
                    if (entries.isNotEmpty()) {
                        appendLine("Staged diff summary:")
                        entries.forEach { entry ->
                            appendLine("- ${entry.changeType.name}: ${entry.oldPath} -> ${entry.newPath}")
                        }
                    }
                }

                Result.success(diffSummary.trim())
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load staged changes summary for $repositoryPath" }
            Result.failure(e)
        }
    }

}

internal fun <C : TransportCommand<C, *>> configureTransportAuthentication(
    command: C,
    authConfig: GitAuthConfig
): C {
    when (authConfig.authType) {
        AuthType.HTTPS, AuthType.TOKEN -> {
            val credentialsProvider = UsernamePasswordCredentialsProvider(
                authConfig.username ?: "git",
                authConfig.password ?: authConfig.token ?: ""
            )
            command.setCredentialsProvider(credentialsProvider)
        }
        AuthType.SSH -> {
            command.setTransportConfigCallback(createSshTransportConfigCallback(authConfig))
        }
    }

    return command
}

internal fun createSshTransportConfigCallback(authConfig: GitAuthConfig): TransportConfigCallback {
    val sshSessionFactory = buildSshSessionFactory(authConfig)

    return TransportConfigCallback { transport: Transport ->
        if (transport is SshTransport) {
            transport.sshSessionFactory = sshSessionFactory
        }
    }
}

internal fun buildSshSessionFactory(authConfig: GitAuthConfig) =
    SshdSessionFactoryBuilder()
        .apply {
            val homeDirectory = File(System.getProperty("user.home", ".")).absoluteFile
            setHomeDirectory(homeDirectory)
            setSshDirectory(File(homeDirectory, ".ssh"))

            authConfig.sshKeyPath?.let { sshKeyPath ->
                val keyFile = File(sshKeyPath).absoluteFile
                setPreferredAuthentications("publickey")
                setDefaultIdentities { listOf(keyFile.toPath()) }
            }
        }
        .build(null)

private fun Throwable.rootCauseMessage(): String {
    val rootCause = generateSequence(this) { it.cause }.last()
    return rootCause.message ?: this.message ?: this.javaClass.simpleName
}

class GitCloneException(message: String, cause: Throwable? = null) : Exception(message, cause)
class GitBranchException(message: String, cause: Throwable? = null) : Exception(message, cause)
