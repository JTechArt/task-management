package com.aitask.core.infrastructure.ide

import com.aitask.core.domain.model.TaskContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

object TaskContextFileWriter {
    fun write(workspacePath: String, taskContext: TaskContext) {
        try {
            val contextFile = File(workspacePath, "TASK_CONTEXT.md")
            val content = buildString {
                appendLine("# Task: ${taskContext.title}")
                appendLine()
                appendLine("## Task ID")
                appendLine(taskContext.taskId)
                appendLine()
                appendLine("## Project")
                appendLine(taskContext.projectName)
                appendLine()
                if (taskContext.repositoryPath != null) {
                    appendLine("## Repository path")
                    appendLine(taskContext.repositoryPath)
                    appendLine()
                }
                if (taskContext.description != null) {
                    appendLine("## Description")
                    appendLine(taskContext.description)
                    appendLine()
                }
                if (taskContext.branchName != null) {
                    appendLine("## Branch")
                    appendLine(taskContext.branchName)
                    appendLine()
                }
                if (taskContext.activeBmadTools.isNotEmpty()) {
                    appendLine("## Active BMAD Tools")
                    taskContext.activeBmadTools.forEach { tool ->
                        appendLine("- $tool")
                    }
                    appendLine()
                }
            }
            contextFile.writeText(content)
            logger.info { "Generated task context file: ${contextFile.absolutePath}" }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to generate task context file" }
        }
    }
}
