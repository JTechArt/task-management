package com.aitask.core.data.repository

import com.aitask.core.data.entity.McpServerConfigurations
import com.aitask.core.domain.model.McpServerConfigurationRequest
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

class McpServerConfigurationRepositoryImplTest {
    @Test
    fun `save persists mcp server settings across repository instances`() = runTest {
        val tempDir = Files.createTempDirectory("mcp-settings-repo-test")
        val dbPath = tempDir.resolve("mcp-settings-db").toString()
        val jdbcUrl = "jdbc:h2:file:$dbPath;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"

        val database = Database.connect(url = jdbcUrl, driver = "org.h2.Driver")
        transaction(database) {
            SchemaUtils.create(McpServerConfigurations)
        }

        val repository1 = McpServerConfigurationRepositoryImpl()
        val saved = repository1.save(
            McpServerConfigurationRequest(
                isEnabled = true,
                port = 4123
            )
        )

        assertTrue(saved.isEnabled)
        assertEquals(4123, saved.port)

        val repository2 = McpServerConfigurationRepositoryImpl()
        val loaded = repository2.find()

        assertNotNull(loaded)
        assertTrue(loaded!!.isEnabled)
        assertEquals(4123, loaded.port)
        assertEquals("http://127.0.0.1:4123/mcp", loaded.endpointUrl)

        transaction(database) {
            assertTrue(McpServerConfigurations.selectAll().count() > 0)
        }
    }
}
