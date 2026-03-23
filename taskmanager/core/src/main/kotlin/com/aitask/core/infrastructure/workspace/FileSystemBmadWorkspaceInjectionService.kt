package com.aitask.core.infrastructure.workspace

import com.aitask.core.domain.service.BmadInjectionResult
import com.aitask.core.domain.service.BmadWorkspaceInjectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString

class FileSystemBmadWorkspaceInjectionService(
    private val configuredBundlePath: String? = System.getProperty("aitask.bmad.bundlePath"),
    private val overwriteExisting: Boolean = System.getProperty("aitask.bmad.overwriteExisting")?.toBoolean() ?: false,
    private val workingDirectory: Path = File(System.getProperty("user.dir", ".")).toPath()
) : BmadWorkspaceInjectionService {

    override suspend fun injectIntoWorkspace(workspacePath: String): Result<BmadInjectionResult> = withContext(Dispatchers.IO) {
        try {
            val bundleRoot = resolveBundleRoot()
                ?: return@withContext Result.failure(
                    BmadInjectionException(
                        "BMAD bundle source not found. Set -Daitask.bmad.bundlePath or run the app from a workspace containing AGENTS.md and .bmad-core."
                    )
                )

            val workspaceDir = File(workspacePath)
            require(workspaceDir.exists() && workspaceDir.isDirectory) {
                "Workspace directory does not exist: $workspacePath"
            }

            val copiedPaths = mutableListOf<String>()
            val skippedPaths = mutableListOf<String>()

            copyEntry(
                source = bundleRoot.resolve(".bmad-core").toFile(),
                destination = File(workspaceDir, ".bmad-core"),
                copiedPaths = copiedPaths,
                skippedPaths = skippedPaths
            )
            copyEntry(
                source = bundleRoot.resolve("AGENTS.md").toFile(),
                destination = File(workspaceDir, "AGENTS.md"),
                copiedPaths = copiedPaths,
                skippedPaths = skippedPaths
            )

            Result.success(
                BmadInjectionResult(
                    applied = copiedPaths.isNotEmpty(),
                    sourcePath = bundleRoot.absolutePathString(),
                    copiedPaths = copiedPaths,
                    skippedPaths = skippedPaths,
                    overwriteExisting = overwriteExisting
                )
            )
        } catch (e: Exception) {
            Result.failure(BmadInjectionException("Failed to inject BMAD bundle: ${e.message}", e))
        }
    }

    private fun resolveBundleRoot(): Path? {
        configuredBundlePath?.takeIf { it.isNotBlank() }?.let { configured ->
            val configuredPath = File(configured).toPath().toAbsolutePath().normalize()
            return if (isValidBundleRoot(configuredPath)) configuredPath else null
        }

        return generateSequence(workingDirectory.toAbsolutePath().normalize()) { current ->
            current.parent
        }.firstOrNull(::isValidBundleRoot)
    }

    private fun isValidBundleRoot(path: Path): Boolean {
        return path.resolve(".bmad-core").toFile().isDirectory &&
            path.resolve("AGENTS.md").toFile().isFile
    }

    private fun copyEntry(
        source: File,
        destination: File,
        copiedPaths: MutableList<String>,
        skippedPaths: MutableList<String>
    ) {
        require(source.exists()) { "Required BMAD bundle content is missing: ${source.name}" }

        if (destination.exists() && !overwriteExisting) {
            skippedPaths += destination.name
            return
        }

        if (source.isDirectory) {
            copyDirectory(source.toPath(), destination.toPath())
        } else {
            destination.parentFile?.mkdirs()
            Files.copy(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            )
        }
        copiedPaths += destination.name
    }

    private fun copyDirectory(source: Path, destination: Path) {
        Files.walk(source).use { stream ->
            stream.forEach { sourcePath ->
                val relative = source.relativize(sourcePath)
                val target = destination.resolve(relative)
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    Files.copy(
                        sourcePath,
                        target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                    )
                }
            }
        }
    }
}

class BmadInjectionException(message: String, cause: Throwable? = null) : Exception(message, cause)
