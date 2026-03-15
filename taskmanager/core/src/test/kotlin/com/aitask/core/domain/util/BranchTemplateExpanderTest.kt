package com.aitask.core.domain.util

import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.model.TaskType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class BranchTemplateExpanderTest {
    
    private fun createTestTask(): Task {
        return Task(
            id = UUID.fromString("12345678-1234-1234-1234-123456789012"),
            title = "Add User Authentication Feature",
            description = "Implement OAuth2 authentication",
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = UUID.randomUUID(),
            workspacePath = null,
            branchName = null,
            createdAt = Instant.parse("2026-03-15T10:00:00Z"),
            updatedAt = Instant.parse("2026-03-15T10:00:00Z"),
            completedAt = null
        )
    }
    
    @Test
    fun `should expand taskId placeholder`() {
        val task = createTestTask()
        val template = "task-{taskId}"
        
        val result = BranchTemplateExpander.expand(template, task)
        
        assertEquals("task-12345678-1234-1234-1234-123456789012", result)
    }
    
    @Test
    fun `should expand taskIdShort placeholder`() {
        val task = createTestTask()
        val template = "task-{taskIdShort}"
        
        val result = BranchTemplateExpander.expand(template, task)
        
        assertEquals("task-12345678", result)
    }
    
    @Test
    fun `should expand taskTitle placeholder with sanitization`() {
        val task = createTestTask()
        val template = "feature/{taskTitle}"
        
        val result = BranchTemplateExpander.expand(template, task)
        
        assertEquals("feature/add-user-authentication-feature", result)
    }
    
    @Test
    fun `should expand taskType placeholder`() {
        val task = createTestTask()
        val template = "{taskType}/{taskIdShort}"
        
        val result = BranchTemplateExpander.expand(template, task)
        
        assertEquals("feature/12345678", result)
    }
    
    @Test
    fun `should expand date placeholder`() {
        val task = createTestTask()
        val template = "task-{date}-{taskIdShort}"
        
        val result = BranchTemplateExpander.expand(template, task)
        
        assertTrue(result.startsWith("task-2026-03-15-"))
    }
    
    @Test
    fun `should expand multiple placeholders`() {
        val task = createTestTask()
        val template = "{taskType}/{taskIdShort}-{taskTitle}"
        
        val result = BranchTemplateExpander.expand(template, task)
        
        assertEquals("feature/12345678-add-user-authentication-feature", result)
    }
    
    @Test
    fun `should sanitize special characters in title`() {
        val task = createTestTask().copy(title = "Fix Bug #123: API Error!")
        val template = "{taskTitle}"
        
        val result = BranchTemplateExpander.expand(template, task)
        
        assertEquals("fix-bug-123-api-error", result)
    }
    
    @Test
    fun `should remove consecutive hyphens`() {
        val task = createTestTask().copy(title = "Fix   Multiple   Spaces")
        val template = "{taskTitle}"
        
        val result = BranchTemplateExpander.expand(template, task)
        
        assertEquals("fix-multiple-spaces", result)
    }
    
    @Test
    fun `should limit title length to 50 characters`() {
        val task = createTestTask().copy(
            title = "This is a very long task title that should be truncated to fit within the branch name length limit"
        )
        val template = "{taskTitle}"
        
        val result = BranchTemplateExpander.expand(template, task)
        
        assertTrue(result.length <= 50)
    }
    
    @Test
    fun `should validate empty template`() {
        val errors = BranchTemplateExpander.validateTemplate("")
        
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("cannot be empty"))
    }
    
    @Test
    fun `should validate template with spaces`() {
        val errors = BranchTemplateExpander.validateTemplate("task {taskId}")
        
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("cannot contain spaces"))
    }
    
    @Test
    fun `should validate unknown placeholders`() {
        val errors = BranchTemplateExpander.validateTemplate("task-{unknownPlaceholder}")
        
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Unknown placeholders"))
    }
    
    @Test
    fun `should validate correct template`() {
        val errors = BranchTemplateExpander.validateTemplate("task-{taskId}")
        
        assertTrue(errors.isEmpty())
    }
    
    @Test
    fun `should provide supported placeholders`() {
        val placeholders = BranchTemplateExpander.getSupportedPlaceholders()
        
        assertTrue(placeholders.containsKey("{taskId}"))
        assertTrue(placeholders.containsKey("{taskIdShort}"))
        assertTrue(placeholders.containsKey("{taskTitle}"))
        assertTrue(placeholders.containsKey("{taskType}"))
        assertTrue(placeholders.containsKey("{date}"))
        assertTrue(placeholders.containsKey("{timestamp}"))
    }
}

