package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RuleRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertTrue

class AttachRuleUseCaseTest {
    
    private val ruleRepository = mockk<RuleRepository>()
    private val projectRepository = mockk<ProjectRepository>()
    private val useCase = AttachRuleUseCase(ruleRepository, projectRepository)
    
    @Test
    fun `should attach rule to project when valid`() = runTest {
        // Given
        val ruleId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        val rule = Rule(
            id = ruleId,
            name = "Test Rule",
            content = "Content",
            category = null,
            scope = RuleScope.PROJECT,
            targetIDE = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val project = Project(
            id = projectId,
            name = "Test Project",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            tags = emptyList(),
            team = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val request = AttachRuleRequest(ruleId, projectId)
        
        coEvery { ruleRepository.findById(ruleId) } returns rule
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { ruleRepository.findByProject(projectId) } returns emptyList()
        coEvery { ruleRepository.attachToProject(projectId, ruleId) } returns ProjectRule(
            projectId, ruleId, Instant.now()
        )
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { ruleRepository.attachToProject(projectId, ruleId) }
    }
    
    @Test
    fun `should fail when rule not found`() = runTest {
        // Given
        val ruleId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val request = AttachRuleRequest(ruleId, projectId)
        
        coEvery { ruleRepository.findById(ruleId) } returns null
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuleNotFoundException)
    }
    
    @Test
    fun `should fail when rule is archived`() = runTest {
        // Given
        val ruleId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        val rule = Rule(
            id = ruleId,
            name = "Archived Rule",
            content = "Content",
            category = null,
            scope = RuleScope.PROJECT,
            targetIDE = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            archivedAt = Instant.now()
        )
        
        val request = AttachRuleRequest(ruleId, projectId)
        
        coEvery { ruleRepository.findById(ruleId) } returns rule
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ArchivedRuleException)
    }
    
    @Test
    fun `should fail when project not found`() = runTest {
        // Given
        val ruleId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        val rule = Rule(
            id = ruleId,
            name = "Test Rule",
            content = "Content",
            category = null,
            scope = RuleScope.PROJECT,
            targetIDE = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val request = AttachRuleRequest(ruleId, projectId)
        
        coEvery { ruleRepository.findById(ruleId) } returns rule
        coEvery { projectRepository.findById(projectId) } returns null
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProjectNotFoundException)
    }
    
    @Test
    fun `should fail when rule already attached`() = runTest {
        // Given
        val ruleId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        val rule = Rule(
            id = ruleId,
            name = "Test Rule",
            content = "Content",
            category = null,
            scope = RuleScope.PROJECT,
            targetIDE = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val project = Project(
            id = projectId,
            name = "Test Project",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            tags = emptyList(),
            team = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val request = AttachRuleRequest(ruleId, projectId)
        
        coEvery { ruleRepository.findById(ruleId) } returns rule
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { ruleRepository.findByProject(projectId) } returns listOf(rule)
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuleAlreadyAttachedException)
    }
}

