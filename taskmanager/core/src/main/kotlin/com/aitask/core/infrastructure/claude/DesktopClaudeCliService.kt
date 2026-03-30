package com.aitask.core.infrastructure.claude

import com.aitask.core.domain.service.ClaudeCliService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Desktop implementation: resolves `claude` on PATH when [cliPath] is blank, validates with
 * [--version], and opens a terminal (macOS Terminal, Windows cmd, or Linux x-terminal).
 */
class DesktopClaudeCliService : ClaudeCliService {
    private val osName: String = System.getProperty("os.name").lowercase()
    private val isMacOS: Boolean = osName.contains("mac")
    private val isWindows: Boolean = osName.contains("win")
    private val isLinux: Boolean = osName.contains("nux")

    override suspend fun resolveExecutable(cliPath: String?): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = cliPath?.trim().orEmpty()
        if (trimmed.isNotEmpty()) {
            val file = File(trimmed)
            return@withContext when {
                !file.exists() -> Result.failure(IllegalStateException("Claude CLI path does not exist: $trimmed"))
                file.isDirectory -> Result.failure(IllegalStateException("Claude CLI path is a directory: $trimmed"))
                else -> Result.success(file.absolutePath)
            }
        }
        return@withContext resolveFromSystemPath()
    }

    private fun resolveFromSystemPath(): Result<String> {
        return try {
            val command = if (isWindows) {
                listOf("cmd", "/c", "where claude")
            } else {
                listOf("sh", "-c", "command -v claude")
            }
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(15, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return Result.failure(
                    IllegalStateException("Claude CLI not found on PATH (lookup timed out). Install Claude CLI or set the executable path in Settings.")
                )
            }
            if (process.exitValue() != 0) {
                return Result.failure(
                    IllegalStateException("Claude CLI not found on PATH. Install Claude CLI or set the executable path in Settings.")
                )
            }
            val output = process.inputStream.bufferedReader().readText().trim()
            val lines = output.lines().filter { it.isNotBlank() }
            val first = lines.firstOrNull()
                ?: return Result.failure(
                    IllegalStateException("Claude CLI not found on PATH. Install Claude CLI or set the executable path in Settings.")
                )
            Result.success(first.trim())
        } catch (e: Exception) {
            logger.warn(e) { "Failed to resolve claude on PATH" }
            Result.failure(
                IllegalStateException("Claude CLI not found on PATH. Install Claude CLI or set the executable path in Settings.", e)
            )
        }
    }

    override suspend fun validateExecutable(resolvedPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(resolvedPath)
            if (!file.exists()) {
                return@withContext Result.failure(IllegalStateException("Claude CLI not found: $resolvedPath"))
            }
            if (!isWindows && !file.canExecute()) {
                return@withContext Result.failure(IllegalStateException("Claude CLI is not executable: $resolvedPath"))
            }
            val versionProbe = ProcessBuilder(listOf(resolvedPath, "--version"))
                .redirectErrorStream(true)
                .start()
            val finished = versionProbe.waitFor(20, TimeUnit.SECONDS)
            if (!finished) {
                versionProbe.destroyForcibly()
                return@withContext Result.failure(IllegalStateException("Claude CLI did not respond to --version in time."))
            }
            if (versionProbe.exitValue() == 0) {
                return@withContext Result.success(Unit)
            }
            val helpProbe = ProcessBuilder(listOf(resolvedPath, "--help"))
                .redirectErrorStream(true)
                .start()
            val helpFinished = helpProbe.waitFor(20, TimeUnit.SECONDS)
            if (!helpFinished) {
                helpProbe.destroyForcibly()
                return@withContext Result.failure(IllegalStateException("Claude CLI validation timed out."))
            }
            if (helpProbe.exitValue() == 0) {
                return@withContext Result.success(Unit)
            }
            Result.failure(IllegalStateException("Claude CLI failed validation (exit ${versionProbe.exitValue()})."))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to validate Claude CLI: ${e.message}", e))
        }
    }

    override suspend fun launchInTerminal(
        resolvedExecutable: String,
        workspacePath: String,
        environment: Map<String, String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val workspace = File(workspacePath)
            if (!workspace.isDirectory) {
                return@withContext Result.failure(IllegalStateException("Workspace does not exist: $workspacePath"))
            }
            return@withContext when {
                isMacOS -> launchMacTerminal(workspace, resolvedExecutable, environment)
                isWindows -> launchWindowsTerminal(workspace, resolvedExecutable, environment)
                isLinux -> launchLinuxTerminal(workspace, resolvedExecutable, environment)
                else -> launchLinuxTerminal(workspace, resolvedExecutable, environment)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to launch Claude in terminal" }
            Result.failure(IllegalStateException("Failed to launch Claude: ${e.message}", e))
        }
    }

    private fun launchMacTerminal(
        workspace: File,
        resolvedExecutable: String,
        environment: Map<String, String>
    ): Result<Unit> {
        val script = writeUnixLaunchScript(workspace, resolvedExecutable, environment)
        val scriptPath = script.absolutePath.replace("'", "'\\''")
        val appleScript = "tell application \"Terminal\" to do script \"bash '$scriptPath'\""
        ProcessBuilder("osascript", "-e", appleScript).start()
        return Result.success(Unit)
    }

    private fun launchWindowsTerminal(
        workspace: File,
        resolvedExecutable: String,
        environment: Map<String, String>
    ): Result<Unit> {
        val script = writeWindowsLaunchScript(workspace, resolvedExecutable, environment)
        ProcessBuilder("cmd", "/c", "start", "cmd", "/k", "call", "\"" + script.absolutePath + "\"").start()
        return Result.success(Unit)
    }

    private fun launchLinuxTerminal(
        workspace: File,
        resolvedExecutable: String,
        environment: Map<String, String>
    ): Result<Unit> {
        val script = writeUnixLaunchScript(workspace, resolvedExecutable, environment)
        val scriptPath = script.absolutePath
        val terminalCommands = listOf(
            listOf("/usr/bin/gnome-terminal", "--", "bash", scriptPath),
            listOf("/usr/bin/konsole", "-e", "bash", scriptPath),
            listOf("/usr/bin/xterm", "-e", "bash", scriptPath)
        )
        for (command in terminalCommands) {
            if (!Files.isExecutable(Paths.get(command[0]))) continue
            try {
                ProcessBuilder(command).start()
                return Result.success(Unit)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to start ${command[0]}" }
            }
        }
        val pb = ProcessBuilder("bash", scriptPath)
        pb.directory(workspace)
        environment.forEach { (key, value) -> pb.environment()[key] = value }
        pb.start()
        return Result.success(Unit)
    }

    private fun writeUnixLaunchScript(
        workspace: File,
        resolvedExecutable: String,
        environment: Map<String, String>
    ): File {
        val dir = File(workspace, ".aitask")
        dir.mkdirs()
        val script = File(dir, "launch-claude.sh")
        val sb = StringBuilder()
        sb.appendLine("#!/usr/bin/env bash")
        sb.appendLine("set -e")
        sb.appendLine("cd \"${workspace.absolutePath.replace("\"", "\\\"")}\"")
        environment.forEach { (key, value) ->
            sb.appendLine("export $key=${bashSingleQuoted(value)}")
        }
        sb.appendLine("exec \"${resolvedExecutable.replace("\"", "\\\"")}\"")
        script.writeText(sb.toString())
        script.setExecutable(true)
        return script
    }

    private fun writeWindowsLaunchScript(
        workspace: File,
        resolvedExecutable: String,
        environment: Map<String, String>
    ): File {
        val dir = File(workspace, ".aitask")
        dir.mkdirs()
        val script = File(dir, "launch-claude.bat")
        val sb = StringBuilder()
        sb.appendLine("@echo off")
        sb.appendLine("cd /d \"${workspace.absolutePath}\"")
        environment.forEach { (key, value) ->
            val escaped = value.replace("\"", "\"\"")
            sb.appendLine("set \"$key=$escaped\"")
        }
        sb.appendLine("\"$resolvedExecutable\"")
        script.writeText(sb.toString())
        return script
    }

    private fun bashSingleQuoted(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
