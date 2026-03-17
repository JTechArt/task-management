package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.DataExportBundle
import com.aitask.core.domain.model.ImportResult
import com.aitask.core.domain.repository.ActivityRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class RestoreFromBackupUseCaseTest {

    private val importDataUseCase = mockk<ImportDataUseCase>()
    private val activityRepository = mockk<ActivityRepository>(relaxed = true)
    private val useCase = RestoreFromBackupUseCase(importDataUseCase, activityRepository)

    @Test
    fun `validateIntegrity returns BackupValidation for valid backup`() {
        val bundle = DataExportBundle(
            schemaVersion = 1,
            exportedAt = Instant.now().toString(),
            projects = listOf(
                com.aitask.core.domain.model.ProjectExportItem(
                    id = "p1",
                    name = "P",
                    workspacePath = "/ws"
                )
            ),
            tasks = emptyList(),
            repositories = emptyList(),
            rules = emptyList(),
            projectRules = emptyList()
        )
        val json = Json { prettyPrint = true }.encodeToString(DataExportBundle.serializer(), bundle)
        val result = useCase.validateIntegrity(json)
        assertTrue(result.isSuccess)
        val validation = result.getOrThrow()
        assertEquals(1, validation.schemaVersion)
        assertEquals(1, validation.projectCount)
        assertEquals(0, validation.taskCount)
    }

    @Test
    fun `validateIntegrity fails for invalid JSON`() {
        val result = useCase.validateIntegrity("not valid json")
        assertTrue(result.isFailure)
    }

    @Test
    fun `validateIntegrity fails for unsupported schema version`() {
        val bundle = DataExportBundle(
            schemaVersion = 99,
            exportedAt = Instant.now().toString(),
            projects = emptyList(),
            tasks = emptyList(),
            repositories = emptyList(),
            rules = emptyList(),
            projectRules = emptyList()
        )
        val json = Json { prettyPrint = true }.encodeToString(DataExportBundle.serializer(), bundle)
        val result = useCase.validateIntegrity(json)
        assertTrue(result.isFailure)
    }

    @Test
    fun `apply validates then imports and records RESTORE_COMPLETED activity`() = runTest {
        val bundle = DataExportBundle(
            schemaVersion = 1,
            exportedAt = Instant.now().toString(),
            projects = emptyList(),
            tasks = emptyList(),
            repositories = emptyList(),
            rules = emptyList(),
            projectRules = emptyList()
        )
        val json = Json { prettyPrint = true }.encodeToString(DataExportBundle.serializer(), bundle)
        val importResult = ImportResult(
            projectsCreated = 0,
            tasksCreated = 0,
            reposCreated = 0,
            rulesCreated = 0,
            projectRulesLinked = 0,
            conflicts = emptyList(),
            errors = emptyList()
        )
        coEvery { importDataUseCase.apply(json) } returns Result.success(importResult)
        val result = useCase.apply(json)
        assertTrue(result.isSuccess)
        assertEquals(importResult, result.getOrThrow())
        coVerify(atLeast = 1) {
            activityRepository.create(
                match { it.type == ActivityType.RESTORE_COMPLETED && it.entityType == "restore" }
            )
        }
    }

    @Test
    fun `apply fails when backup integrity invalid`() = runTest {
        val result = useCase.apply("invalid")
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { importDataUseCase.apply(any()) }
    }
}
