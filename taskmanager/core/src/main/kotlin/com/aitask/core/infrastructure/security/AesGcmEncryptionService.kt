package com.aitask.core.infrastructure.security

import com.aitask.core.domain.service.EncryptionService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private val logger = KotlinLogging.logger {}
private const val ALGORITHM = "AES/GCM/NoPadding"
private const val TAG_LENGTH_BITS = 128
private const val IV_LENGTH_BYTES = 12
private const val KEY_LENGTH_BYTES = 32

/**
 * AES-256-GCM encryption for OAuth tokens.
 * Key from AITASK_OAUTH_ENCRYPTION_KEY (hex, 64 chars = 32 bytes). If missing, encryption unavailable.
 */
class AesGcmEncryptionService(
    private val keyHex: String? = System.getenv("AITASK_OAUTH_ENCRYPTION_KEY")
) : EncryptionService {
    private val key: SecretKeySpec? by lazy {
        keyHex?.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F' } }
            ?.let { hex ->
                SecretKeySpec(hexToBytes(hex), "AES")
            }
            ?: run {
                logger.warn { "AITASK_OAUTH_ENCRYPTION_KEY not set or invalid (need 64 hex chars). OAuth token encryption disabled." }
                null
            }
    }

    override fun isAvailable(): Boolean = key != null

    override fun encrypt(plaintext: String): Result<String> = runCatching {
        val k = key ?: throw IllegalStateException("Encryption key not configured")
        val iv = ByteArray(IV_LENGTH_BYTES).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, k, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        Base64.getEncoder().encodeToString(iv + encrypted)
    }

    override fun decrypt(ciphertext: String): Result<String> = runCatching {
        val k = key ?: throw IllegalStateException("Encryption key not configured")
        val bytes = Base64.getDecoder().decode(ciphertext)
        require(bytes.size >= IV_LENGTH_BYTES) { "Invalid ciphertext" }
        val iv = bytes.copyOfRange(0, IV_LENGTH_BYTES)
        val encrypted = bytes.copyOfRange(IV_LENGTH_BYTES, bytes.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, k, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun hexToBytes(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
