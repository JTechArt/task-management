package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.repository.ActivityRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CreateBackupUseCaseTest {

    @TempDir
    lateinit var tempDir: java.io.File

    private val exportDataUseCase = mockk<ExportDataUseCase>()
    private val activityRepository = mockk<ActivityRepository>(relaxed = true)
    private val useCase = CreateBackupUseCase(exportDataUseCase, activityRepository)

    @Test
    fun `invoke writes export to file and records BACKUP_CREATED activity`() = runTest {
        val targetFile = File(tempDir, "backup.json")
        val exportJson = """{"schemaVersion":1,"exportedAt":"2025-01-01T00:00:00Z","projects":[],"tasks":[]}"""
        coEvery { exportDataUseCase(any()) } returns Result.success(exportJson)
        val result = useCase(targetFile, includeArchived = false)
        assertTrue(result.isSuccess)
        assertEquals(targetFile, result.getOrThrow())
        assertTrue(targetFile.exists())
        assertEquals(exportJson, targetFile.readText())
        coVerify(atLeast = 1) {
            activityRepository.create(
                match { it.type == ActivityType.BACKUP_CREATED && it.entityType == "backup" }
            )
        }
    }

    @Test
    fun `invoke records FAILED activity when export fails`() = runTest {
        val targetFile = File(tempDir, "backup.json")
        coEvery { exportDataUseCase(any()) } returns Result.failure(RuntimeException("Export error"))
        val result = useCase(targetFile, includeArchived = false)
        assertTrue(result.isFailure)
        coVerify(atLeast = 1) {
            activityRepository.create(
                match { it.type == ActivityType.BACKUP_CREATED && it.entityType == "backup" && it.status == com.aitask.core.domain.model.ActivityStatus.FAILED }
            )
        }
    }
}
