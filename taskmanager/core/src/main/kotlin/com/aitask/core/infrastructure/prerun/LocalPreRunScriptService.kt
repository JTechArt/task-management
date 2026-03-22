package com.aitask.core.infrastructure.prerun

import com.aitask.core.domain.model.PreRunExecutionResult
import com.aitask.core.domain.model.EnvironmentCheckTemplateRegistry
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
                templateCommand(PreRunScriptType.NODE_VERSION, required)
            }

            PreRunScriptType.JAVA_VERSION -> {
                val required = requireRequiredValue(script)
                templateCommand(PreRunScriptType.JAVA_VERSION, required)
            }

            PreRunScriptType.PYTHON_VERSION -> {
                val required = requireRequiredValue(script)
                templateCommand(PreRunScriptType.PYTHON_VERSION, required)
            }

            PreRunScriptType.ENVIRONMENT_VARIABLE -> {
                val required = requireRequiredValue(script)
                templateCommand(PreRunScriptType.ENVIRONMENT_VARIABLE, required)
            }

            PreRunScriptType.DEPENDENCY_PRESENT -> {
                val required = requireRequiredValue(script)
                templateCommand(PreRunScriptType.DEPENDENCY_PRESENT, required)
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

    private fun templateCommand(type: PreRunScriptType, value: String): CommandSpec {
        require(value.matches(Regex("^[a-zA-Z0-9._-]+$"))) {
            "Template parameter value must be alphanumeric (letters, numbers, dots, underscores, hyphens)"
        }
        val template = requireNotNull(EnvironmentCheckTemplateRegistry.findByType(type)) {
            "No environment check template registered for $type"
        }
        return shellCommand(template.generatePreview(value, isWindows()))
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
