package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.RuleRepository
import com.aitask.core.domain.validation.RuleValidator
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertTrue

class UpdateRuleUseCaseTest {
    
    private val ruleRepository = mockk<RuleRepository>()
    private val ruleValidator = RuleValidator()
    private val useCase = UpdateRuleUseCase(ruleRepository, ruleValidator)
    
    @Test
    fun `should update rule when valid`() = runTest {
        // Given
        val ruleId = UUID.randomUUID()
        val existing = Rule(
            id = ruleId,
            name = "Old Name",
            content = "Old content",
            category = RuleCategory.CODING_STANDARDS,
            scope = RuleScope.GLOBAL,
            targetIDE = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val request = UpdateRuleRequest(
            id = ruleId,
            name = "New Name",
            content = null,
            category = null,
            scope = null,
            targetIDE = null
        )
        
        coEvery { ruleRepository.findById(ruleId) } returns existing
        coEvery { ruleRepository.update(any()) } answers { firstArg() }
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isSuccess)
        val updated = result.getOrNull()!!
        assertTrue(updated.name == "New Name")
        assertTrue(updated.content == "Old content")
    }
    
    @Test
    fun `should fail when rule not found`() = runTest {
        // Given
        val ruleId = UUID.randomUUID()
        val request = UpdateRuleRequest(
            id = ruleId,
            name = "New Name",
            content = null,
            category = null,
            scope = null,
            targetIDE = null
        )
        
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
        val existing = Rule(
            id = ruleId,
            name = "Archived Rule",
            content = "Content",
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            archivedAt = Instant.now()
        )
        
        val request = UpdateRuleRequest(
            id = ruleId,
            name = "New Name",
            content = null,
            category = null,
            scope = null,
            targetIDE = null
        )
        
        coEvery { ruleRepository.findById(ruleId) } returns existing
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ArchivedRuleException)
    }
    
    @Test
    fun `should fail when no fields provided`() = runTest {
        // Given
        val ruleId = UUID.randomUUID()
        val request = UpdateRuleRequest(
            id = ruleId,
            name = null,
            content = null,
            category = null,
            scope = null,
            targetIDE = null
        )
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
    }
}

