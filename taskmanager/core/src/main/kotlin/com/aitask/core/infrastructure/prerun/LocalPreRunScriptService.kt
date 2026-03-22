package com.aitask.core.infrastructure.prerun

import com.aitask.core.domain.model.PreRunExecutionResult
import com.aitask.core.domain.model.PreRunScript
import com.aitask.core.domain.model.PreRunScriptExecutionException
import com.aitask.core.domain.model.PreRunScriptType
import com.aitask.core.domain.service.PreRunScriptService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val logger = KotlinLogging.logger {}

class LocalPreRunScriptService : PreRunScriptService {

    override suspend fun executeScripts(
        scripts: List<PreRunScript>,
        workingDirectory: String
    ): Result<List<PreRunExecutionResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val workspaceDir = File(workingDirectory)
            require(workspaceDir.exists() && workspaceDir.isDirectory) {
                "Pre-run working directory does not exist: $workingDirectory"
            }

            val results = mutableListOf<PreRunExecutionResult>()
            for (script in scripts.sortedBy { it.executionOrder }) {
                val result = executeSingleScript(script, workspaceDir)
                results += result
                if (!result.succeeded) {
                    throw PreRunScriptExecutionException(result)
                }
            }
            results
        }
    }

    private fun executeSingleScript(
        script: PreRunScript,
        workspaceDir: File
    ): PreRunExecutionResult {
        val spec = buildCommand(script, workspaceDir)
        logger.info { "Executing pre-run script '${script.name}' with command '${spec.description}'" }

        val process = ProcessBuilder(spec.command)
            .directory(workspaceDir)
            .redirectErrorStream(false)
            .start()

        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        return PreRunExecutionResult(
            script = script,
            succeeded = exitCode == 0,
            exitCode = exitCode,
            stdout = stdout,
            stderr = stderr,
            resolvedCommand = spec.description
        )
    }

    private fun buildCommand(script: PreRunScript, workspaceDir: File): CommandSpec {
        return when (script.type) {
            PreRunScriptType.INLINE_COMMAND -> {
                val command = requireNotNull(script.inlineScript?.takeIf { it.isNotBlank() }) {
                    "Inline command is required for ${script.name}"
                }
                shellCommand(command)
            }

            PreRunScriptType.SCRIPT_PATH -> {
                val rawPath = requireNotNull(script.scriptPath?.takeIf { it.isNotBlank() }) {
                    "Script path is required for ${script.name}"
                }
                val resolved = File(rawPath).let { if (it.isAbsolute) it else File(workspaceDir, rawPath) }
                require(resolved.exists()) { "Script path does not exist: ${resolved.absolutePath}" }
                require(isPathWithinWorkspace(resolved, workspaceDir)) {
                    "Script path must be within workspace: ${resolved.absolutePath}"
                }
                shellCommand(resolved.absolutePath)
            }

            PreRunScriptType.NODE_VERSION -> {
                val required = requireRequiredValue(script)
                shellCommand("node --version | grep -E '^v?$required([.]|$)'")
            }

            PreRunScriptType.JAVA_VERSION -> {
                val required = requireRequiredValue(script)
                shellCommand("java -version 2>&1 | grep -F '$required'")
            }

            PreRunScriptType.PYTHON_VERSION -> {
                val required = requireRequiredValue(script)
                shellCommand("(python --version 2>&1 || python3 --version 2>&1) | grep -F '$required'")
            }

            PreRunScriptType.ENVIRONMENT_VARIABLE -> {
                val required = requireRequiredValue(script)
                shellCommand("if [ -n \"${'$'}$required\" ]; then exit 0; else echo 'Missing required environment variable: $required' >&2; exit 1; fi")
            }
        }
    }

    private fun requireRequiredValue(script: PreRunScript): String =
        requireNotNull(script.requiredValue?.takeIf { it.isNotBlank() }) {
            "Required value is missing for ${script.name}"
        }

    private fun shellCommand(command: String): CommandSpec {
        return if (isWindows()) {
            CommandSpec(listOf("cmd", "/c", command), command)
        } else {
            CommandSpec(listOf("bash", "-lc", command), command)
        }
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").lowercase().contains("win")

    private fun isPathWithinWorkspace(scriptFile: File, workspaceDir: File): Boolean {
        val scriptCanonical = scriptFile.canonicalFile
        val workspaceCanonical = workspaceDir.canonicalFile
        val workspacePath = workspaceCanonical.absolutePath
        val scriptPath = scriptCanonical.absolutePath
        return scriptPath == workspacePath || scriptPath.startsWith(workspacePath + File.separator)
    }

    private data class CommandSpec(
        val command: List<String>,
        val description: String
    )
}
