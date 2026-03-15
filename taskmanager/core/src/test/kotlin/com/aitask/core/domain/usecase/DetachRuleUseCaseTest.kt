package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.DetachRuleRequest
import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.RuleScope
import com.aitask.core.domain.repository.RuleRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertTrue

class DetachRuleUseCaseTest {
    
    private val ruleRepository = mockk<RuleRepository>()
    private val useCase = DetachRuleUseCase(ruleRepository)
    
    @Test
    fun `should detach rule from project when attached`() = runTest {
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
        
        val request = DetachRuleRequest(ruleId, projectId)
        
        coEvery { ruleRepository.findById(ruleId) } returns rule
        coEvery { ruleRepository.findByProject(projectId) } returns listOf(rule)
        coEvery { ruleRepository.detachFromProject(projectId, ruleId) } just Runs
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { ruleRepository.detachFromProject(projectId, ruleId) }
    }
    
    @Test
    fun `should fail when rule not found`() = runTest {
        // Given
        val ruleId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val request = DetachRuleRequest(ruleId, projectId)
        
        coEvery { ruleRepository.findById(ruleId) } returns null
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuleNotFoundException)
    }
    
    @Test
    fun `should fail when rule not attached to project`() = runTest {
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
        
        val request = DetachRuleRequest(ruleId, projectId)
        
        coEvery { ruleRepository.findById(ruleId) } returns rule
        coEvery { ruleRepository.findByProject(projectId) } returns emptyList()
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuleNotAttachedException)
    }
}

