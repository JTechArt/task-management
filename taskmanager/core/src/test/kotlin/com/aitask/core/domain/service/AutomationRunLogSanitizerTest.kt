package com.aitask.core.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutomationRunLogSanitizerTest {
    @Test
    fun `sanitizeErrorMessage redacts bearer tokens`() {
        val input: String = "Request failed: Bearer abc123secret and more"
        val actual: String = AutomationRunLogSanitizer.sanitizeErrorMessage(input)
        assertFalse(actual.contains("abc123secret"))
        assertTrue(actual.contains("[REDACTED]"))
    }

    @Test
    fun `sanitizeErrorMessage redacts sk- style keys`() {
        val input: String = "key sk-abcdefghijklmnopqrstuvwxyz012345 failed"
        val actual: String = AutomationRunLogSanitizer.sanitizeErrorMessage(input)
        assertFalse(actual.contains("sk-abc"))
    }

    @Test
    fun `truncateForLog respects max length`() {
        val long: String = "x".repeat(50)
        val actual: String = AutomationRunLogSanitizer.truncateForLog(long, 10)
        assertEquals(11, actual.length)
        assertTrue(actual.endsWith("…"))
    }
}
