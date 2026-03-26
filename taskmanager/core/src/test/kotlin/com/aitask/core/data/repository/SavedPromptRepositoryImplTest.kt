package com.aitask.core.data.repository

import com.aitask.core.data.entity.Projects
import com.aitask.core.data.entity.SavedPrompts
import com.aitask.core.domain.model.AgentScope
import com.aitask.core.domain.model.SavedPromptRequest
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant
import java.util.UUID

class SavedPromptRepositoryImplTest {

    @Test
    fun `save persists global and project-scoped prompts`() = runTest {
        val db = createDatabase("saved-prompts-basic")
        transaction(db) { SchemaUtils.create(Projects, SavedPrompts) }
        val projectId = insertProject(db)

        val repository = SavedPromptRepositoryImpl()

        val global = repository.save(
            SavedPromptRequest(
                name = "My Global Prompt",
                content = "Summarize {{taskTitle}}",
                category = "Summarization",
                scope = AgentScope.GLOBAL
            )
        )
        val projectScoped = repository.save(
            SavedPromptRequest(
                name = "Project Prompt",
                content = "Help with {{projectName}}",
                scope = AgentScope.PROJECT,
                projectId = projectId
            )
        )

        assertNotNull(global.id)
        assertEquals("My Global Prompt", global.name)
        assertEquals("Summarization", global.category)
        assertEquals(AgentScope.GLOBAL, global.scope)
        assertNull(global.projectId)

        assertEquals("Project Prompt", projectScoped.name)
        assertEquals(AgentScope.PROJECT, projectScoped.scope)
        assertEquals(projectId, projectScoped.projectId)

        assertEquals(2, repository.findAll().size)
        assertEquals(1, repository.findGlobal().size)
        assertEquals(1, repository.findByProject(projectId).size)
        assertEquals(2, repository.findAvailableForProject(projectId).size)
        assertEquals(1, repository.findAvailableForProject(null).size)
    }

    @Test
    fun `save updates existing prompt on second call with same id`() = runTest {
        val db = createDatabase("saved-prompts-update")
        transaction(db) { SchemaUtils.create(Projects, SavedPrompts) }

        val repository = SavedPromptRepositoryImpl()

        val first = repository.save(
            SavedPromptRequest(name = "Original", content = "Original content", scope = AgentScope.GLOBAL)
        )
        val updated = repository.save(
            SavedPromptRequest(id = first.id, name = "Updated", content = "Updated content", scope = AgentScope.GLOBAL)
        )

        assertEquals(first.id, updated.id)
        assertEquals("Updated", updated.name)
        assertEquals("Updated content", updated.content)

        transaction(db) {
            assertEquals(1L, SavedPrompts.selectAll().count())
        }
    }

    @Test
    fun `findById returns null when id does not exist`() = runTest {
        val db = createDatabase("saved-prompts-find-by-id")
        transaction(db) { SchemaUtils.create(Projects, SavedPrompts) }

        val repository = SavedPromptRepositoryImpl()
        assertNull(repository.findById(UUID.randomUUID()))
    }

    @Test
    fun `delete removes prompt and returns true`() = runTest {
        val db = createDatabase("saved-prompts-delete")
        transaction(db) { SchemaUtils.create(Projects, SavedPrompts) }

        val repository = SavedPromptRepositoryImpl()
        val saved = repository.save(
            SavedPromptRequest(name = "To Delete", content = "Delete me", scope = AgentScope.GLOBAL)
        )

        val deleted = repository.delete(saved.id)

        assertTrue(deleted)
        assertTrue(repository.findAll().isEmpty())
    }

    @Test
    fun `delete returns false when id does not exist`() = runTest {
        val db = createDatabase("saved-prompts-delete-missing")
        transaction(db) { SchemaUtils.create(Projects, SavedPrompts) }

        val repository = SavedPromptRepositoryImpl()
        assertFalse(repository.delete(UUID.randomUUID()))
    }

    @Test
    fun `category is stored as null when blank`() = runTest {
        val db = createDatabase("saved-prompts-category")
        transaction(db) { SchemaUtils.create(Projects, SavedPrompts) }

        val repository = SavedPromptRepositoryImpl()
        val saved = repository.save(
            SavedPromptRequest(name = "No Category", content = "content", category = "  ", scope = AgentScope.GLOBAL)
        )
        assertNull(saved.category)
    }

    private fun createDatabase(name: String): Database {
        val tempDir = Files.createTempDirectory("$name-test")
        val dbPath = tempDir.resolve("db").toString()
        val jdbcUrl = "jdbc:h2:file:$dbPath;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
        return Database.connect(url = jdbcUrl, driver = "org.h2.Driver")
    }

    private fun insertProject(db: Database): UUID {
        val projectId = UUID.randomUUID()
        transaction(db) {
            Projects.insert {
                it[id] = projectId
                it[name] = "Test Project"
                it[workspacePath] = "/workspace"
                it[branchTemplate] = "task-{taskId}"
                it[methodology] = "NONE"
                it[createdAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
        }
        return projectId
    }
}
