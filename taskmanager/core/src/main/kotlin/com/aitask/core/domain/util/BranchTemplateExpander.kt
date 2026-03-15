package com.aitask.core.domain.util

import com.aitask.core.domain.model.Task
import java.time.format.DateTimeFormatter

/**
 * Utility for expanding branch name templates with task data.
 * 
 * Supported placeholders:
 * - {taskId} - Full task UUID
 * - {taskIdShort} - First 8 characters of task UUID
 * - {taskTitle} - Task title (sanitized for branch names)
 * - {taskType} - Task type (lowercase)
 * - {date} - Current date in YYYY-MM-DD format
 * - {timestamp} - Unix timestamp
 */
object BranchTemplateExpander {
    
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    /**
     * Expands a branch template with task data.
     * 
     * @param template The branch template (e.g., "task-{taskId}")
     * @param task The task to use for expansion
     * @return The expanded branch name
     */
    fun expand(template: String, task: Task): String {
        var expanded = template
        
        // Replace {taskId} with full UUID
        expanded = expanded.replace("{taskId}", task.id.toString())
        
        // Replace {taskIdShort} with first 8 characters
        expanded = expanded.replace("{taskIdShort}", task.id.toString().substring(0, 8))
        
        // Replace {taskTitle} with sanitized title
        expanded = expanded.replace("{taskTitle}", sanitizeForBranchName(task.title))
        
        // Replace {taskType} with lowercase task type
        expanded = expanded.replace("{taskType}", task.taskType.name.lowercase())
        
        // Replace {date} with current date
        expanded = expanded.replace("{date}", task.createdAt.atZone(java.time.ZoneId.systemDefault()).format(DATE_FORMATTER))
        
        // Replace {timestamp} with Unix timestamp
        expanded = expanded.replace("{timestamp}", task.createdAt.epochSecond.toString())
        
        return expanded
    }
    
    /**
     * Sanitizes a string for use in a Git branch name.
     * 
     * Rules:
     * - Converts to lowercase
     * - Replaces spaces with hyphens
     * - Removes special characters except hyphens and underscores
     * - Removes consecutive hyphens
     * - Trims hyphens from start and end
     * - Limits length to 50 characters
     * 
     * @param input The string to sanitize
     * @return The sanitized string
     */
    fun sanitizeForBranchName(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[\\s]+"), "-")  // Replace spaces with hyphens
            .replace(Regex("[^a-z0-9\\-_]"), "")  // Remove special characters
            .replace(Regex("-+"), "-")  // Remove consecutive hyphens
            .trim('-', '_')  // Trim hyphens and underscores from edges
            .take(50)  // Limit length
    }
    
    /**
     * Validates a branch template.
     * 
     * @param template The template to validate
     * @return List of validation errors (empty if valid)
     */
    fun validateTemplate(template: String): List<String> {
        val errors = mutableListOf<String>()
        
        if (template.isBlank()) {
            errors.add("Branch template cannot be empty")
            return errors
        }
        
        // Check for invalid characters
        if (template.contains(Regex("[\\s]"))) {
            errors.add("Branch template cannot contain spaces")
        }
        
        // Check for known placeholders
        val knownPlaceholders = setOf(
            "{taskId}",
            "{taskIdShort}",
            "{taskTitle}",
            "{taskType}",
            "{date}",
            "{timestamp}"
        )
        
        // Find all placeholders in template
        val placeholderPattern = Regex("\\{[^}]+\\}")
        val foundPlaceholders = placeholderPattern.findAll(template).map { it.value }.toSet()
        
        // Check for unknown placeholders
        val unknownPlaceholders = foundPlaceholders - knownPlaceholders
        if (unknownPlaceholders.isNotEmpty()) {
            errors.add("Unknown placeholders: ${unknownPlaceholders.joinToString(", ")}")
        }
        
        return errors
    }
    
    /**
     * Gets a list of supported placeholders with descriptions.
     * 
     * @return Map of placeholder to description
     */
    fun getSupportedPlaceholders(): Map<String, String> {
        return mapOf(
            "{taskId}" to "Full task UUID",
            "{taskIdShort}" to "First 8 characters of task UUID",
            "{taskTitle}" to "Task title (sanitized for branch names)",
            "{taskType}" to "Task type (lowercase)",
            "{date}" to "Current date in YYYY-MM-DD format",
            "{timestamp}" to "Unix timestamp"
        )
    }
}

