package com.aitask.core.data.repository

import com.aitask.core.data.entity.AgentDefinitions
import com.aitask.core.data.entity.LlmConfigurations
import com.aitask.core.data.entity.Projects
import com.aitask.core.domain.model.AgentDefinitionRequest
import com.aitask.core.domain.model.AgentScope
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant
import java.util.UUID

class AgentDefinitionRepositoryImplTest {
    @Test
    fun `save persists global and project-scoped agents`() {
        runTest {
        val tempDir = Files.createTempDirectory("agent-definition-repo-test")
        val dbPath = tempDir.resolve("agent-definition-db").toString()
        val jdbcUrl = "jdbc:h2:file:$dbPath;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"

        val database = Database.connect(url = jdbcUrl, driver = "org.h2.Driver")
        transaction(database) {
            SchemaUtils.create(Projects, LlmConfigurations, AgentDefinitions)
            Projects.insert {
                it[id] = PROJECT_ID
                it[name] = "Atlas"
                it[workspacePath] = "/workspace"
                it[branchTemplate] = "task-{taskId}"
                it[methodology] = "NONE"
                it[createdAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
        }

        val repository = AgentDefinitionRepositoryImpl()
        val global = repository.save(
            AgentDefinitionRequest(
                id = GLOBAL_ID,
                name = "Summarize Task",
                promptTemplate = "Summarize {{taskTitle}}",
                scope = AgentScope.GLOBAL
            )
        )
        val projectScoped = repository.save(
            AgentDefinitionRequest(
                id = PROJECT_AGENT_ID,
                name = "Project Helper",
                promptTemplate = "Help {{projectName}}",
                scope = AgentScope.PROJECT,
                projectId = PROJECT_ID
            )
        )

        assertTrue(global.isEnabled)
        assertEquals(2, repository.findAll().size)
        assertEquals(1, repository.findGlobal().size)
        assertEquals(1, repository.findByProject(PROJECT_ID).size)
        assertEquals("Project Helper", projectScoped.name)
        assertEquals(2, repository.findAvailableForProject(PROJECT_ID).size)
        assertEquals(1, repository.findAvailableForProject(null).size)
        }
    }

    companion object {
        private val PROJECT_ID = UUID.randomUUID()
        private val GLOBAL_ID = UUID.randomUUID()
        private val PROJECT_AGENT_ID = UUID.randomUUID()
    }
}
