package com.aitask.core.infrastructure.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AesGcmEncryptionServiceTest {

    private val validKeyHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    @Test
    fun `isAvailable returns false when key not configured`() {
        val service = AesGcmEncryptionService(keyHex = null)
        assertFalse(service.isAvailable())
    }

    @Test
    fun `isAvailable returns false when key length invalid`() {
        val service = AesGcmEncryptionService(keyHex = "short")
        assertFalse(service.isAvailable())
    }

    @Test
    fun `isAvailable returns false when key contains non-hex chars`() {
        val service = AesGcmEncryptionService(keyHex = "g".repeat(64))
        assertFalse(service.isAvailable())
    }

    @Test
    fun `isAvailable returns true when valid key provided`() {
        val service = AesGcmEncryptionService(keyHex = validKeyHex)
        assertTrue(service.isAvailable())
    }

    @Test
    fun `encrypt and decrypt roundtrip produces same plaintext`() {
        val service = AesGcmEncryptionService(keyHex = validKeyHex)
        val plaintext = "sensitive-oauth-token-123"
        val encrypted = service.encrypt(plaintext)
        assertTrue(encrypted.isSuccess)
        val decrypted = service.decrypt(encrypted.getOrThrow())
        assertTrue(decrypted.isSuccess)
        assertEquals(plaintext, decrypted.getOrThrow())
    }

    @Test
    fun `encrypt produces different ciphertext each time due to random IV`() {
        val service = AesGcmEncryptionService(keyHex = validKeyHex)
        val plaintext = "token"
        val c1 = service.encrypt(plaintext).getOrThrow()
        val c2 = service.encrypt(plaintext).getOrThrow()
        assertTrue(c1 != c2)
        assertEquals(plaintext, service.decrypt(c1).getOrThrow())
        assertEquals(plaintext, service.decrypt(c2).getOrThrow())
    }

    @Test
    fun `encrypt fails when key not configured`() {
        val service = AesGcmEncryptionService(keyHex = null)
        val result = service.encrypt("token")
        assertTrue(result.isFailure)
    }

    @Test
    fun `decrypt fails when key not configured`() {
        val service = AesGcmEncryptionService(keyHex = null)
        val result = service.decrypt("dGVzdA==")
        assertTrue(result.isFailure)
    }

    @Test
    fun `decrypt fails for invalid base64`() {
        val service = AesGcmEncryptionService(keyHex = validKeyHex)
        val result = service.decrypt("not-valid-base64!!!")
        assertTrue(result.isFailure)
    }
}
