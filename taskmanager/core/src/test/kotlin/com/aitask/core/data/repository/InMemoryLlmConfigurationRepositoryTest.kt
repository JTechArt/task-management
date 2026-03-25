package com.aitask.core.data.repository

import com.aitask.core.domain.model.LlmConfigurationRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InMemoryLlmConfigurationRepositoryTest {
    @Test
    fun `save stores configurations and maintains a default`() = runTest {
        val repository = InMemoryLlmConfigurationRepository()

        val first = repository.save(
            LlmConfigurationRequest(
                name = "Local Ollama",
                endpointUrl = "http://localhost:11434",
                modelIdentifier = "llama3.1",
                apiKey = "token-1",
                isDefault = true
            )
        )

        val second = repository.save(
            LlmConfigurationRequest(
                name = "Backup Model",
                endpointUrl = "http://localhost:1234",
                modelIdentifier = "mistral",
                isDefault = false
            )
        )

        assertTrue(first.isDefault)
        assertFalse(second.isDefault)
        assertEquals(2, repository.findAll().size)
        assertNotNull(repository.findDefault())
        assertTrue(repository.findById(first.id)?.hasApiKey == true)
    }

    @Test
    fun `setDefault moves default flag to selected configuration`() = runTest {
        val repository = InMemoryLlmConfigurationRepository()
        val first = repository.save(
            LlmConfigurationRequest(
                name = "First",
                endpointUrl = "http://localhost:11434",
                modelIdentifier = "llama3.1"
            )
        )
        val second = repository.save(
            LlmConfigurationRequest(
                name = "Second",
                endpointUrl = "http://localhost:11435",
                modelIdentifier = "qwen2.5"
            )
        )

        repository.setDefault(second.id)

        assertFalse(repository.findById(first.id)?.isDefault == true)
        assertTrue(repository.findById(second.id)?.isDefault == true)
    }
}
