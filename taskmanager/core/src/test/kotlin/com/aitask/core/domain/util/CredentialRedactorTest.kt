package com.aitask.core.domain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CredentialRedactorTest {

    @Test
    fun `redactUrl returns unchanged when URL has no embedded credentials`() {
        val url = "https://github.com/org/repo.git"
        assertEquals(url, CredentialRedactor.redactUrl(url))
    }

    @Test
    fun `redactUrl redacts username and password from URL`() {
        val url = "https://user:secret@github.com/org/repo.git"
        assertEquals(
            "https://***@github.com/org/repo.git",
            CredentialRedactor.redactUrl(url)
        )
    }

    @Test
    fun `redactUrl redacts token as username`() {
        val url = "https://ghp_token123@github.com/org/repo.git"
        assertEquals(
            "https://***@github.com/org/repo.git",
            CredentialRedactor.redactUrl(url)
        )
    }

    @Test
    fun `redactMessage redacts credential URLs within message`() {
        val message = "Clone failed: https://user:pass@github.com/repo.git - Permission denied"
        assertEquals(
            "Clone failed: https://***@github.com/repo.git - Permission denied",
            CredentialRedactor.redactMessage(message)
        )
    }

    @Test
    fun `redactMessage returns unchanged when no credential URL present`() {
        val message = "Clone failed: Permission denied"
        assertEquals(message, CredentialRedactor.redactMessage(message))
    }
}
