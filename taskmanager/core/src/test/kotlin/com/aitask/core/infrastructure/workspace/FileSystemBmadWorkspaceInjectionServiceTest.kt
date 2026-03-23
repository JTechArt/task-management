package com.aitask.core.infrastructure.workspace

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class FileSystemBmadWorkspaceInjectionServiceTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should copy bmad bundle into workspace`() = runTest {
        val bundleRoot = tempDir.resolve("bundle").toFile().apply {
            mkdirs()
            File(this, "AGENTS.md").writeText("# Agents")
            File(this, ".bmad-core").mkdirs()
            File(this, ".bmad-core/core-config.yaml").writeText("test: true")
        }
        val workspace = tempDir.resolve("workspace").toFile().apply { mkdirs() }

        val service = FileSystemBmadWorkspaceInjectionService(
            configuredBundlePath = bundleRoot.absolutePath
        )

        val result = service.injectIntoWorkspace(workspace.absolutePath)

        assertTrue(result.isSuccess)
        val injection = result.getOrThrow()
        assertTrue(File(workspace, "AGENTS.md").isFile)
        assertTrue(File(workspace, ".bmad-core/core-config.yaml").isFile)
        assertEquals(listOf(".bmad-core", "AGENTS.md"), injection.copiedPaths)
    }

    @Test
    fun `should skip existing content when overwrite is disabled`() = runTest {
        val bundleRoot = tempDir.resolve("bundle").toFile().apply {
            mkdirs()
            File(this, "AGENTS.md").writeText("new agents")
            File(this, ".bmad-core").mkdirs()
            File(this, ".bmad-core/core-config.yaml").writeText("new config")
        }
        val workspace = tempDir.resolve("workspace").toFile().apply {
            mkdirs()
            File(this, "AGENTS.md").writeText("existing agents")
            File(this, ".bmad-core").mkdirs()
            File(this, ".bmad-core/core-config.yaml").writeText("existing config")
        }

        val service = FileSystemBmadWorkspaceInjectionService(
            configuredBundlePath = bundleRoot.absolutePath,
            overwriteExisting = false
        )

        val result = service.injectIntoWorkspace(workspace.absolutePath)

        assertTrue(result.isSuccess)
        val injection = result.getOrThrow()
        assertEquals(listOf(".bmad-core", "AGENTS.md"), injection.skippedPaths)
        assertEquals("existing agents", File(workspace, "AGENTS.md").readText())
        assertEquals("existing config", File(workspace, ".bmad-core/core-config.yaml").readText())
    }

    @Test
    fun `should fail when bundle source is missing`() = runTest {
        val workspace = tempDir.resolve("workspace").toFile().apply { mkdirs() }
        val service = FileSystemBmadWorkspaceInjectionService(
            configuredBundlePath = tempDir.resolve("missing-bundle").toString()
        )

        val result = service.injectIntoWorkspace(workspace.absolutePath)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("BMAD bundle source not found"))
    }
}
