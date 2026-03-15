package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.RuleScope
import com.aitask.core.domain.repository.RuleRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertTrue

class DeleteRuleUseCaseTest {
    
    private val ruleRepository = mockk<RuleRepository>()
    private val useCase = DeleteRuleUseCase(ruleRepository)
    
    @Test
    fun `should archive rule when exists`() = runTest {
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
        coEvery { ruleRepository.update(any()) } answers { firstArg() }
        
        // When
        val result = useCase(ruleId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { ruleRepository.update(match { it.archivedAt != null }) }
    }
    
    @Test
    fun `should fail when rule not found`() = runTest {
        // Given
        val ruleId = UUID.randomUUID()
        coEvery { ruleRepository.findById(ruleId) } returns null
        
        // When
        val result = useCase(ruleId)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuleNotFoundException)
        coVerify(exactly = 0) { ruleRepository.update(any()) }
    }
}

