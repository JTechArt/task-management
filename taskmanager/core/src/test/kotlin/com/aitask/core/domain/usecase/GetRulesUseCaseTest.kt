package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.RuleScope
import com.aitask.core.domain.repository.RuleRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetRulesUseCaseTest {
    
    private val ruleRepository = mockk<RuleRepository>()
    private val useCase = GetRulesUseCase(ruleRepository)
    
    @Test
    fun `should get all rules excluding archived`() = runTest {
        // Given
        val activeRule = Rule(
            id = UUID.randomUUID(),
            name = "Active Rule",
            content = "Content",
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val archivedRule = Rule(
            id = UUID.randomUUID(),
            name = "Archived Rule",
            content = "Content",
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            archivedAt = Instant.now()
        )
        
        coEvery { ruleRepository.findAll() } returns listOf(activeRule, archivedRule)
        
        // When
        val result = useCase.getAll(includeArchived = false)
        
        // Then
        assertTrue(result.isSuccess)
        val rules = result.getOrNull()!!
        assertEquals(1, rules.size)
        assertEquals("Active Rule", rules[0].name)
    }
    
    @Test
    fun `should get all rules including archived when requested`() = runTest {
        // Given
        val activeRule = Rule(
            id = UUID.randomUUID(),
            name = "Active Rule",
            content = "Content",
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val archivedRule = Rule(
            id = UUID.randomUUID(),
            name = "Archived Rule",
            content = "Content",
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            archivedAt = Instant.now()
        )
        
        coEvery { ruleRepository.findAll() } returns listOf(activeRule, archivedRule)
        
        // When
        val result = useCase.getAll(includeArchived = true)
        
        // Then
        assertTrue(result.isSuccess)
        val rules = result.getOrNull()!!
        assertEquals(2, rules.size)
    }
    
    @Test
    fun `should get rules by scope`() = runTest {
        // Given
        val globalRule = Rule(
            id = UUID.randomUUID(),
            name = "Global Rule",
            content = "Content",
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        coEvery { ruleRepository.findByScope(RuleScope.GLOBAL) } returns listOf(globalRule)
        
        // When
        val result = useCase.getByScope(RuleScope.GLOBAL)
        
        // Then
        assertTrue(result.isSuccess)
        val rules = result.getOrNull()!!
        assertEquals(1, rules.size)
        assertEquals(RuleScope.GLOBAL, rules[0].scope)
    }
    
    @Test
    fun `should get rule by id`() = runTest {
        // Given
        val ruleId = UUID.randomUUID()
        val rule = Rule(
            id = ruleId,
            name = "Test Rule",
            content = "Content",
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        coEvery { ruleRepository.findById(ruleId) } returns rule
        
        // When
        val result = useCase.getById(ruleId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(ruleId, result.getOrNull()!!.id)
    }
    
    @Test
    fun `should fail when rule not found by id`() = runTest {
        // Given
        val ruleId = UUID.randomUUID()
        coEvery { ruleRepository.findById(ruleId) } returns null
        
        // When
        val result = useCase.getById(ruleId)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuleNotFoundException)
    }
}

