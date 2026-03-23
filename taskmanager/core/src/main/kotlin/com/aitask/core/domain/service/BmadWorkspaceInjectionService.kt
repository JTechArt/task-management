package com.aitask.core.domain.service

data class BmadInjectionResult(
    val applied: Boolean,
    val sourcePath: String?,
    val copiedPaths: List<String>,
    val skippedPaths: List<String>,
    val overwriteExisting: Boolean
)

interface BmadWorkspaceInjectionService {
    suspend fun injectIntoWorkspace(workspacePath: String): Result<BmadInjectionResult>
}
