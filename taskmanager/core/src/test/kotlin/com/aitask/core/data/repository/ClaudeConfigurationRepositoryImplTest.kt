package com.aitask.core.data.repository

import com.aitask.core.data.entity.ClaudeConfigurations
import com.aitask.core.domain.model.ClaudeConfigurationRequest
import com.aitask.core.domain.model.ClaudeIntegrationMode
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

class ClaudeConfigurationRepositoryImplTest {
    private val encryptionKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    @Test
    fun `save persists claude configuration`() = runTest {
        val tempDir = Files.createTempDirectory("claude-config-repo-test")
        val dbPath = tempDir.resolve("claude-config-db").toString()
        val jdbcUrl = "jdbc:h2:file:$dbPath;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
        val database = Database.connect(url = jdbcUrl, driver = "org.h2.Driver")
        transaction(database) {
            SchemaUtils.create(ClaudeConfigurations)
        }
        val repository = ClaudeConfigurationRepositoryImpl(AesGcmEncryptionService(encryptionKey))
        val saved = repository.save(
            ClaudeConfigurationRequest(
                isEnabled = true,
                integrationMode = ClaudeIntegrationMode.CLI,
                cliPath = "/usr/local/bin/claude",
                apiBaseUrl = null,
                apiKey = "sk-ant-test"
            )
        )
        assertTrue(saved.isEnabled)
        assertEquals(ClaudeIntegrationMode.CLI, saved.integrationMode)
        assertEquals("/usr/local/bin/claude", saved.cliPath)
        assertTrue(saved.hasApiKey)
        val loaded = repository.find()
        assertNotNull(loaded)
        assertEquals(saved.id, loaded!!.id)
        val key = repository.findApiKey(saved.id)
        assertEquals("sk-ant-test", key)
        transaction(database) {
            assertEquals(1L, ClaudeConfigurations.selectAll().count())
        }
    }

    @Test
    fun `save updates existing singleton row`() = runTest {
        val tempDir = Files.createTempDirectory("claude-config-upsert-test")
        val dbPath = tempDir.resolve("claude-config-db").toString()
        val jdbcUrl = "jdbc:h2:file:$dbPath;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
        val database = Database.connect(url = jdbcUrl, driver = "org.h2.Driver")
        transaction(database) {
            SchemaUtils.create(ClaudeConfigurations)
        }
        val repository = ClaudeConfigurationRepositoryImpl(AesGcmEncryptionService(encryptionKey))
        val first = repository.save(
            ClaudeConfigurationRequest(
                isEnabled = true,
                integrationMode = ClaudeIntegrationMode.API,
                cliPath = "",
                apiBaseUrl = "https://api.anthropic.com",
                apiKey = "k1"
            )
        )
        val second = repository.save(
            ClaudeConfigurationRequest(
                id = first.id,
                isEnabled = false,
                integrationMode = ClaudeIntegrationMode.API,
                cliPath = "",
                apiBaseUrl = null,
                apiKey = null
            )
        )
        assertEquals(first.id, second.id)
        assertFalse(second.isEnabled)
        transaction(database) {
            assertEquals(1L, ClaudeConfigurations.selectAll().count())
        }
    }
}
