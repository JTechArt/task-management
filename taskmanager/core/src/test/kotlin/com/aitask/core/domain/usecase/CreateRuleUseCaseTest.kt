package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.RuleRepository
import com.aitask.core.domain.validation.RuleValidator
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CreateRuleUseCaseTest {
    
    private val ruleRepository = mockk<RuleRepository>()
    private val ruleValidator = RuleValidator()
    private val useCase = CreateRuleUseCase(ruleRepository, ruleValidator)
    
    @Test
    fun `should create rule when valid`() = runTest {
        // Given
        val request = CreateRuleRequest(
            name = "Test Rule",
            content = "Test content",
            category = RuleCategory.CODING_STANDARDS,
            scope = RuleScope.GLOBAL,
            targetIDE = null
        )
        
        coEvery { ruleRepository.create(any()) } answers { firstArg() }
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { ruleRepository.create(any()) }
    }
    
    @Test
    fun `should fail when name is blank`() = runTest {
        // Given
        val request = CreateRuleRequest(
            name = "",
            content = "Test content",
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null
        )
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        coVerify(exactly = 0) { ruleRepository.create(any()) }
    }
    
    @Test
    fun `should fail when content is blank`() = runTest {
        // Given
        val request = CreateRuleRequest(
            name = "Test Rule",
            content = "",
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null
        )
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        coVerify(exactly = 0) { ruleRepository.create(any()) }
    }
    
    @Test
    fun `should create rule with all fields`() = runTest {
        // Given
        val request = CreateRuleRequest(
            name = "Complete Rule",
            content = "Detailed content",
            category = RuleCategory.ARCHITECTURE,
            scope = RuleScope.PROJECT,
            targetIDE = IDEType.CURSOR
        )
        
        coEvery { ruleRepository.create(any()) } answers { firstArg() }
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isSuccess)
        val rule = result.getOrNull()!!
        assertTrue(rule.name == "Complete Rule")
        assertTrue(rule.content == "Detailed content")
        assertTrue(rule.category == RuleCategory.ARCHITECTURE)
        assertTrue(rule.scope == RuleScope.PROJECT)
        assertTrue(rule.targetIDE == IDEType.CURSOR)
    }
}

