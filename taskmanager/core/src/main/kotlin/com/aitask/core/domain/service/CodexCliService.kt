package com.aitask.core.domain.service

/**
 * Resolves the Codex CLI executable, validates it, and launches it in a user-visible terminal
 * with the task workspace as the working directory.
 */
interface CodexCliService {
    suspend fun resolveExecutable(cliPath: String?): Result<String>
    suspend fun validateExecutable(resolvedPath: String): Result<Unit>
    suspend fun launchInTerminal(
        resolvedExecutable: String,
        workspacePath: String,
        environment: Map<String, String>
    ): Result<Unit>
}
