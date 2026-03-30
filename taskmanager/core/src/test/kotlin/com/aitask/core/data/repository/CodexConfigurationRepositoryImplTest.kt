package com.aitask.core.data.repository

import com.aitask.core.data.entity.CodexConfigurations
import com.aitask.core.domain.model.CodexConfigurationRequest
import com.aitask.core.infrastructure.security.AesGcmEncryptionService
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class CodexConfigurationRepositoryImplTest {
    private val encryptionKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    @Test
    fun `save persists codex configuration`() = runTest {
        val tempDir = Files.createTempDirectory("codex-config-repo-test")
        val dbPath = tempDir.resolve("codex-config-db").toString()
        val jdbcUrl = "jdbc:h2:file:$dbPath;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
        val database = Database.connect(url = jdbcUrl, driver = "org.h2.Driver")
        transaction(database) {
            SchemaUtils.create(CodexConfigurations)
        }
        val repository = CodexConfigurationRepositoryImpl(AesGcmEncryptionService(encryptionKey))
        val saved = repository.save(
            CodexConfigurationRequest(
                isEnabled = true,
                cliPath = "/usr/local/bin/codex",
                apiKey = "sk-test"
            )
        )
        assertTrue(saved.isEnabled)
        assertEquals("/usr/local/bin/codex", saved.cliPath)
        assertTrue(saved.hasApiKey)
        val loaded = repository.find()
        assertNotNull(loaded)
        assertEquals(saved.id, loaded!!.id)
        val key = repository.findApiKey(saved.id)
        assertEquals("sk-test", key)
        transaction(database) {
            assertEquals(1L, CodexConfigurations.selectAll().count())
        }
    }

    @Test
    fun `save updates existing singleton row`() = runTest {
        val tempDir = Files.createTempDirectory("codex-config-upsert-test")
        val dbPath = tempDir.resolve("codex-config-db").toString()
        val jdbcUrl = "jdbc:h2:file:$dbPath;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
        val database = Database.connect(url = jdbcUrl, driver = "org.h2.Driver")
        transaction(database) {
            SchemaUtils.create(CodexConfigurations)
        }
        val repository = CodexConfigurationRepositoryImpl(AesGcmEncryptionService(encryptionKey))
        val first = repository.save(
            CodexConfigurationRequest(
                isEnabled = true,
                cliPath = "/bin/codex",
                apiKey = "k1"
            )
        )
        val second = repository.save(
            CodexConfigurationRequest(
                id = first.id,
                isEnabled = false,
                cliPath = "",
                apiKey = null
            )
        )
        assertEquals(first.id, second.id)
        assertFalse(second.isEnabled)
        transaction(database) {
            assertEquals(1L, CodexConfigurations.selectAll().count())
        }
    }
}
