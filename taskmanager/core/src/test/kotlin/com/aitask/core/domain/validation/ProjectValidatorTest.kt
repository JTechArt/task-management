package com.aitask.core.domain.validation

import com.aitask.core.domain.model.CreateProjectRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProjectValidatorTest {
    private val validator = ProjectValidator()
    
    @Test
    fun `should pass validation for valid project`() {
        val request = CreateProjectRequest(
            name = "My Project",
            description = "A test project",
            workspacePath = "/home/user/workspace",
            branchTemplate = "task-{taskId}",
            tags = listOf("backend", "kotlin"),
            team = "Engineering"
        )
        
        val result = validator.validate(request)
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should fail when name is blank`() {
        val request = CreateProjectRequest(
            name = "",
            workspacePath = "/home/user/workspace",
            branchTemplate = "task-{taskId}"
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "name" && it.code == "REQUIRED" })
    }
    
    @Test
    fun `should fail when name exceeds max length`() {
        val request = CreateProjectRequest(
            name = "a".repeat(201),
            workspacePath = "/home/user/workspace",
            branchTemplate = "task-{taskId}"
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "name" && it.code == "MAX_LENGTH" })
    }
    
    @Test
    fun `should fail when name contains invalid characters`() {
        val request = CreateProjectRequest(
            name = "My Project @#$",
            workspacePath = "/home/user/workspace",
            branchTemplate = "task-{taskId}"
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "name" && it.code == "INVALID_FORMAT" })
    }
    
    @Test
    fun `should fail when workspace path is blank`() {
        val request = CreateProjectRequest(
            name = "My Project",
            workspacePath = "",
            branchTemplate = "task-{taskId}"
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "workspacePath" && it.code == "REQUIRED" })
    }
    
    @Test
    fun `should fail when branch template is missing taskId placeholder`() {
        val request = CreateProjectRequest(
            name = "My Project",
            workspacePath = "/home/user/workspace",
            branchTemplate = "task-branch"
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "branchTemplate" && it.code == "MISSING_PLACEHOLDER" })
    }
    
    @Test
    fun `should fail when description exceeds max length`() {
        val request = CreateProjectRequest(
            name = "My Project",
            description = "a".repeat(10001),
            workspacePath = "/home/user/workspace",
            branchTemplate = "task-{taskId}"
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "description" && it.code == "MAX_LENGTH" })
    }
    
    @Test
    fun `should collect multiple validation errors`() {
        val request = CreateProjectRequest(
            name = "",
            workspacePath = "",
            branchTemplate = "invalid"
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.size >= 3)
    }
}

