package com.aitask.core.data.repository

import com.aitask.core.data.entity.LlmConfigurations
import com.aitask.core.domain.model.LlmConfigurationRequest
import com.aitask.core.infrastructure.security.AesGcmEncryptionService
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.UUID

class LlmConfigurationRepositoryImplTest {
    private val encryptionKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    @Test
    fun `save persists configurations and default state across repository instances`() = runTest {
        val tempDir = Files.createTempDirectory("llm-config-repo-test")
        val dbPath = tempDir.resolve("llm-config-db").toString()
        val jdbcUrl = "jdbc:h2:file:$dbPath;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"

        val database = Database.connect(url = jdbcUrl, driver = "org.h2.Driver")
        transaction(database) {
            SchemaUtils.create(LlmConfigurations)
        }

        val repository1 = LlmConfigurationRepositoryImpl(AesGcmEncryptionService(encryptionKey))
        val firstId = UUID.randomUUID()
        val secondId = UUID.randomUUID()

        val first = repository1.save(
            LlmConfigurationRequest(
                id = firstId,
                name = "Local Ollama",
                endpointUrl = "http://localhost:11434",
                modelIdentifier = "llama3.1",
                apiKey = "secret-token",
                isDefault = true
            )
        )
        val second = repository1.save(
            LlmConfigurationRequest(
                id = secondId,
                name = "Backup Model",
                endpointUrl = "http://localhost:11435",
                modelIdentifier = "mistral",
                isDefault = false
            )
        )

        assertTrue(first.isDefault)
        assertFalse(second.isDefault)

        transaction(database) {
            val storedKey = LlmConfigurations.selectAll()
                .where { LlmConfigurations.id eq firstId }
                .single()[LlmConfigurations.apiKeyEncrypted]
            assertNotNull(storedKey)
            assertTrue(storedKey != "secret-token")
        }

        val repository2 = LlmConfigurationRepositoryImpl(AesGcmEncryptionService(encryptionKey))
        val allConfigurations = repository2.findAll()

        assertEquals(2, allConfigurations.size)
        assertTrue(allConfigurations.first { it.id == firstId }.isDefault)
        assertFalse(allConfigurations.first { it.id == secondId }.isDefault)
        assertEquals("Local Ollama", repository2.findById(firstId)?.name)
        assertTrue(repository2.findDefault()?.id == firstId)
    }
}
