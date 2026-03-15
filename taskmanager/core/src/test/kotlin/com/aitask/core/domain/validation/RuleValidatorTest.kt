package com.aitask.core.domain.validation

import com.aitask.core.domain.model.CreateRuleRequest
import com.aitask.core.domain.model.RuleCategory
import com.aitask.core.domain.model.RuleScope
import com.aitask.core.domain.model.UpdateRuleRequest
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuleValidatorTest {
    
    private val validator = RuleValidator()
    
    @Test
    fun `should validate valid create request`() {
        // Given
        val request = CreateRuleRequest(
            name = "Test Rule",
            content = "Test content",
            category = RuleCategory.CODING_STANDARDS,
            scope = RuleScope.GLOBAL,
            targetIDE = null
        )
        
        // When
        val result = validator.validate(request)
        
        // Then
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should reject blank name`() {
        // Given
        val request = CreateRuleRequest(
            name = "",
            content = "Test content",
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null
        )
        
        // When
        val result = validator.validate(request)
        
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "name" && it.message.contains("blank") })
    }
    
    @Test
    fun `should reject name exceeding 200 characters`() {
        // Given
        val request = CreateRuleRequest(
            name = "a".repeat(201),
            content = "Test content",
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null
        )
        
        // When
        val result = validator.validate(request)
        
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "name" && it.message.contains("200") })
    }
    
    @Test
    fun `should reject blank content`() {
        // Given
        val request = CreateRuleRequest(
            name = "Test Rule",
            content = "",
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null
        )
        
        // When
        val result = validator.validate(request)
        
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "content" && it.message.contains("blank") })
    }
    
    @Test
    fun `should reject content exceeding 100000 characters`() {
        // Given
        val request = CreateRuleRequest(
            name = "Test Rule",
            content = "a".repeat(100_001),
            category = null,
            scope = RuleScope.GLOBAL,
            targetIDE = null
        )
        
        // When
        val result = validator.validate(request)
        
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "content" && it.message.contains("100,000") })
    }
    
    @Test
    fun `should validate valid update request`() {
        // Given
        val request = UpdateRuleRequest(
            id = UUID.randomUUID(),
            name = "Updated Rule",
            content = null,
            category = null,
            scope = null,
            targetIDE = null
        )
        
        // When
        val result = validator.validate(request)
        
        // Then
        assertTrue(result.isValid)
    }
    
    @Test
    fun `should reject update with no fields`() {
        // Given
        val request = UpdateRuleRequest(
            id = UUID.randomUUID(),
            name = null,
            content = null,
            category = null,
            scope = null,
            targetIDE = null
        )
        
        // When
        val result = validator.validate(request)
        
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "update" })
    }
}

