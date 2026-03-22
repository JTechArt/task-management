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

class EnvironmentTemplateExecutionTest {

    private val service = LocalPreRunScriptService()

    @Test
    fun `should fail with actionable message when dependency is missing`() = runTest {
        val workingDir = Files.createTempDirectory("prerun-dependency").toFile()
        val script = PreRunScript(
            id = UUID.randomUUID(),
            projectId = UUID.randomUUID(),
            name = "Check Missing Dependency",
            type = PreRunScriptType.DEPENDENCY_PRESENT,
            requiredValue = "definitely-not-installed-cli",
            executionOrder = 1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val result = service.executeScripts(listOf(script), workingDir.absolutePath)

        assertTrue(result.isFailure)
        val failure = result.exceptionOrNull() as PreRunScriptExecutionException
        assertTrue(failure.message!!.contains("not found on PATH"))
    }

    @Test
    fun `should reject template parameter with shell metacharacters`() = runTest {
        val workingDir = Files.createTempDirectory("prerun-invalid").toFile()
        val script = PreRunScript(
            id = UUID.randomUUID(),
            projectId = UUID.randomUUID(),
            name = "Invalid Check",
            type = PreRunScriptType.DEPENDENCY_PRESENT,
            requiredValue = "docker; rm -rf /",
            executionOrder = 1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val result = service.executeScripts(listOf(script), workingDir.absolutePath)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("alphanumeric") == true)
    }
}
