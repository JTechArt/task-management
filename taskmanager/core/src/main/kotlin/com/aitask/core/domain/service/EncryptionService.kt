package com.aitask.core.domain.service

/**
 * Encrypts and decrypts sensitive data at rest (AC4).
 * Implementations must use strong encryption (e.g. AES-256-GCM).
 */
interface EncryptionService {
    fun encrypt(plaintext: String): Result<String>
    fun decrypt(ciphertext: String): Result<String>
    fun isAvailable(): Boolean
}
