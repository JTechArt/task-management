package com.aitask.core.data.repository

import com.aitask.core.data.entity.GeppaConfigurations
import com.aitask.core.domain.model.GeppaConfigurationRequest
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class GeppaConfigurationRepositoryImplTest {
    @Test
    fun `save persists geppa configuration across repository instances`() = runTest {
        val tempDir = Files.createTempDirectory("geppa-config-repo-test")
        val dbPath = tempDir.resolve("geppa-config-db").toString()
        val jdbcUrl = "jdbc:h2:file:$dbPath;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"

        val database = Database.connect(url = jdbcUrl, driver = "org.h2.Driver")
        transaction(database) {
            SchemaUtils.create(GeppaConfigurations)
        }

        val repository1 = GeppaConfigurationRepositoryImpl()
        assertNull(repository1.find())

        val saved = repository1.save(
            GeppaConfigurationRequest(
                isEnabled = true,
                endpointUrl = "http://localhost:8080"
            )
        )

        assertTrue(saved.isEnabled)
        assertEquals("http://localhost:8080", saved.endpointUrl)
        assertNotNull(saved.id)

        val repository2 = GeppaConfigurationRepositoryImpl()
        val loaded = repository2.find()

        assertNotNull(loaded)
        assertTrue(loaded!!.isEnabled)
        assertEquals("http://localhost:8080", loaded.endpointUrl)

        transaction(database) {
            assertEquals(1L, GeppaConfigurations.selectAll().count())
        }
    }

    @Test
    fun `save updates existing singleton row rather than inserting a second row`() = runTest {
        val tempDir = Files.createTempDirectory("geppa-config-upsert-test")
        val dbPath = tempDir.resolve("geppa-config-db").toString()
        val jdbcUrl = "jdbc:h2:file:$dbPath;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"

        val database = Database.connect(url = jdbcUrl, driver = "org.h2.Driver")
        transaction(database) {
            SchemaUtils.create(GeppaConfigurations)
        }

        val repository = GeppaConfigurationRepositoryImpl()
        val first = repository.save(
            GeppaConfigurationRequest(isEnabled = true, endpointUrl = "http://localhost:8080")
        )
        val second = repository.save(
            GeppaConfigurationRequest(id = first.id, isEnabled = false, endpointUrl = "")
        )

        assertEquals(first.id, second.id)
        assertFalse(second.isEnabled)

        transaction(database) {
            assertEquals(1L, GeppaConfigurations.selectAll().count())
        }
    }
}
