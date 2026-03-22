package com.aitask.core.infrastructure.prerun

import com.aitask.core.domain.model.PreRunScript
import com.aitask.core.domain.model.PreRunScriptExecutionException
import com.aitask.core.domain.model.PreRunScriptType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant
import java.util.UUID

class LocalPreRunScriptServiceTest {

    private val service = LocalPreRunScriptService()

    @Test
    fun `should execute inline command successfully`() = runTest {
        val workingDir = Files.createTempDirectory("prerun-inline").toFile()
        val script = createScript(
            type = PreRunScriptType.INLINE_COMMAND,
            inlineScript = "echo ready"
        )

        val result = service.executeScripts(listOf(script), workingDir.absolutePath)

        assertTrue(result.isSuccess)
        val execution = result.getOrThrow().single()
        assertTrue(execution.succeeded)
        assertTrue(execution.stdout.contains("ready"))
    }

    @Test
    fun `should fail when required environment variable is missing`() = runTest {
        val workingDir = Files.createTempDirectory("prerun-env").toFile()
        val script = createScript(
            type = PreRunScriptType.ENVIRONMENT_VARIABLE,
            requiredValue = "AITASK_MISSING_TEST_ENV"
        )

        val result = service.executeScripts(listOf(script), workingDir.absolutePath)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PreRunScriptExecutionException)
        val failure = result.exceptionOrNull() as PreRunScriptExecutionException
        assertTrue(failure.message!!.contains("AITASK_MISSING_TEST_ENV"))
    }

    @Test
    fun `should execute script path successfully`() = runTest {
        val workingDir = Files.createTempDirectory("prerun-script").toFile()
        val scriptFile = workingDir.resolve("check.sh").apply {
            writeText("#!/usr/bin/env bash\necho script-ok\n")
            setExecutable(true)
        }
        val script = createScript(
            type = PreRunScriptType.SCRIPT_PATH,
            scriptPath = scriptFile.absolutePath
        )

        val result = service.executeScripts(listOf(script), workingDir.absolutePath)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().single().stdout.contains("script-ok"))
    }

    @Test
    fun `should reject script path outside workspace`() = runTest {
        val workingDir = Files.createTempDirectory("prerun-script").toFile()
        val outsidePath = Files.createTempDirectory("prerun-outside").resolve("outside.sh").toFile().apply {
            writeText("#!/usr/bin/env bash\necho outside\n")
            setExecutable(true)
        }
        val script = createScript(
            type = PreRunScriptType.SCRIPT_PATH,
            scriptPath = outsidePath.absolutePath
        )

        val result = service.executeScripts(listOf(script), workingDir.absolutePath)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error.message!!.contains("within workspace"))
    }

    private fun createScript(
        type: PreRunScriptType,
        scriptPath: String? = null,
        inlineScript: String? = null,
        requiredValue: String? = null
    ) = PreRunScript(
        id = UUID.randomUUID(),
        projectId = UUID.randomUUID(),
        name = "Test Script",
        type = type,
        scriptPath = scriptPath,
        inlineScript = inlineScript,
        requiredValue = requiredValue,
        executionOrder = 1,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}
