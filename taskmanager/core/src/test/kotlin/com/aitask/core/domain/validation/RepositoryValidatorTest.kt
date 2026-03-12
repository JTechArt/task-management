package com.aitask.core.domain.validation

import com.aitask.core.domain.model.AuthType
import com.aitask.core.domain.model.CreateRepositoryRequest
import com.aitask.core.domain.model.GitProvider
import com.aitask.core.domain.model.IDEType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class RepositoryValidatorTest {
    private val validator = RepositoryValidator()
    
    @Test
    fun `should pass validation for valid repository`() {
        val request = CreateRepositoryRequest(
            projectId = UUID.randomUUID(),
            name = "my-repo",
            cloneUrl = "https://github.com/user/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR, IDEType.VS_CODE)
        )
        
        val result = validator.validate(request)
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should fail when name is blank`() {
        val request = CreateRepositoryRequest(
            projectId = UUID.randomUUID(),
            name = "",
            cloneUrl = "https://github.com/user/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR)
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "name" && it.code == "REQUIRED" })
    }
    
    @Test
    fun `should fail when clone URL is blank`() {
        val request = CreateRepositoryRequest(
            projectId = UUID.randomUUID(),
            name = "my-repo",
            cloneUrl = "",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR)
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "cloneUrl" && it.code == "REQUIRED" })
    }
    
    @Test
    fun `should fail when clone URL is invalid`() {
        val request = CreateRepositoryRequest(
            projectId = UUID.randomUUID(),
            name = "my-repo",
            cloneUrl = "invalid-url",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR)
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "cloneUrl" && it.code == "INVALID_URL" })
    }
    
    @Test
    fun `should accept git protocol URLs`() {
        val request = CreateRepositoryRequest(
            projectId = UUID.randomUUID(),
            name = "my-repo",
            cloneUrl = "git://github.com/user/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.SSH,
            preferredIDEs = listOf(IDEType.CURSOR)
        )
        
        val result = validator.validate(request)
        
        assertTrue(result.isValid)
    }
    
    @Test
    fun `should accept ssh protocol URLs`() {
        val request = CreateRepositoryRequest(
            projectId = UUID.randomUUID(),
            name = "my-repo",
            cloneUrl = "ssh://git@github.com/user/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.SSH,
            preferredIDEs = listOf(IDEType.CURSOR)
        )
        
        val result = validator.validate(request)
        
        assertTrue(result.isValid)
    }
    
    @Test
    fun `should fail when no preferred IDEs selected`() {
        val request = CreateRepositoryRequest(
            projectId = UUID.randomUUID(),
            name = "my-repo",
            cloneUrl = "https://github.com/user/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = emptyList()
        )
        
        val result = validator.validate(request)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "preferredIDEs" && it.code == "REQUIRED" })
    }
}

