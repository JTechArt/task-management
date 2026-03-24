package com.aitask.core.domain.validation

import com.aitask.core.domain.model.CreateTaskRequest
import com.aitask.core.domain.model.Methodology
import com.aitask.core.domain.model.TaskType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class TaskValidatorTest {
    private val validator = TaskValidator()
    
    @Test
    fun `should pass validation for valid task`() {
        val request = CreateTaskRequest(
            title = "Implement feature X",
            description = "Add new feature to the system",
            taskType = TaskType.FEATURE,
            projectId = UUID.randomUUID(),
            methodologyOverride = Methodology.BMAD,
            workspacePath = "/workspace/project",
            branchName = "feature/task-123"
        )
        
        val result = validator.validate(request)
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should fail when title is blank`() {
        val request = CreateTaskRequest(
            title = "",
            taskType = TaskType.FEATURE,
            projectId = UUID.randomUUID()
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "title" && it.code == "REQUIRED" })
    }
    
    @Test
    fun `should fail when title exceeds max length`() {
        val request = CreateTaskRequest(
            title = "a".repeat(501),
            taskType = TaskType.FEATURE,
            projectId = UUID.randomUUID()
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "title" && it.code == "MAX_LENGTH" })
    }
    
    @Test
    fun `should fail when description exceeds max length`() {
        val request = CreateTaskRequest(
            title = "Valid title",
            description = "a".repeat(50001),
            taskType = TaskType.FEATURE,
            projectId = UUID.randomUUID()
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "description" && it.code == "MAX_LENGTH" })
    }
    
    @Test
    fun `should fail when workspace path exceeds max length`() {
        val request = CreateTaskRequest(
            title = "Valid title",
            taskType = TaskType.FEATURE,
            projectId = UUID.randomUUID(),
            workspacePath = "a".repeat(1001)
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "workspacePath" && it.code == "MAX_LENGTH" })
    }
    
    @Test
    fun `should fail when branch name contains invalid characters`() {
        val request = CreateTaskRequest(
            title = "Valid title",
            taskType = TaskType.FEATURE,
            projectId = UUID.randomUUID(),
            branchName = "feature/task@#$"
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "branchName" && it.code == "INVALID_FORMAT" })
    }
    
    @Test
    fun `should accept valid branch names`() {
        val validBranchNames = listOf(
            "feature/task-123",
            "bugfix/issue_456",
            "hotfix/critical-fix",
            "feature/user-auth"
        )
        
        validBranchNames.forEach { branchName ->
            val request = CreateTaskRequest(
                title = "Valid title",
                taskType = TaskType.FEATURE,
                projectId = UUID.randomUUID(),
                branchName = branchName
            )
            
            val result = validator.validate(request)
            
            assertTrue(result.isValid, "Branch name '$branchName' should be valid")
        }
    }
    
    @Test
    fun `should pass when optional fields are null`() {
        val request = CreateTaskRequest(
            title = "Valid title",
            taskType = TaskType.FEATURE,
            projectId = UUID.randomUUID()
        )
        
        val result = validator.validate(request)
        
        assertTrue(result.isValid)
    }
}
