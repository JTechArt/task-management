package com.aitask.core.infrastructure.ide

import com.aitask.core.domain.model.IDEType
import com.aitask.core.domain.model.TaskContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.UUID

class DesktopIDEServiceTest {
    
    private lateinit var ideService: DesktopIDEService
    
    @TempDir
    lateinit var tempDir: Path
    
    @BeforeEach
    fun setup() {
        ideService = DesktopIDEService()
    }
    
    @Test
    fun `should detect installed IDEs`() = runTest {
        // When
        val installedIDEs = ideService.detectInstalledIDEs()
        
        // Then
        assertNotNull(installedIDEs)
        // The list might be empty if no IDEs are installed, which is fine for testing
        assertTrue(installedIDEs.all { it.executablePath.isNotEmpty() })
    }
    
    @Test
    fun `should validate IDE path when file exists`() = runTest {
        // Given
        val testFile = File(tempDir.toFile(), "test-ide")
        testFile.createNewFile()
        
        // When
        val isValid = ideService.validateIDEPath(IDEType.CURSOR, testFile.absolutePath)
        
        // Then
        assertTrue(isValid)
    }
    
    @Test
    fun `should not validate IDE path when file does not exist`() = runTest {
        // Given
        val nonExistentPath = "/path/that/does/not/exist/ide"
        
        // When
        val isValid = ideService.validateIDEPath(IDEType.CURSOR, nonExistentPath)
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `should get default IDE path for each IDE type`() = runTest {
        // When/Then - just verify the method doesn't throw
        for (ideType in IDEType.values()) {
            val path = ideService.getDefaultIDEPath(ideType)
            // Path might be null if IDE is not installed, which is fine
            if (path != null) {
                assertTrue(path.isNotEmpty())
            }
        }
    }
    
    @Test
    fun `should fail to launch IDE when workspace does not exist`() = runTest {
        // Given
        val nonExistentWorkspace = "/path/that/does/not/exist"
        
        // When
        val result = ideService.launchIDE(
            ideType = IDEType.CURSOR,
            workspacePath = nonExistentWorkspace
        )
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("does not exist") == true)
    }
    
    @Test
    fun `should create task context file when launching with task context`() = runTest {
        // Given
        val workspace = tempDir.toFile()
        val taskContext = TaskContext(
            taskId = UUID.randomUUID(),
            title = "Test Task",
            description = "Test Description",
            projectName = "Test Project",
            branchName = "feature/test"
        )
        
        // When - This will fail to launch the IDE but should create the context file
        ideService.launchIDE(
            ideType = IDEType.CURSOR,
            workspacePath = workspace.absolutePath,
            taskContext = taskContext
        )
        
        // Then - Check if context file was created
        val contextFile = File(workspace, "TASK_CONTEXT.md")
        assertTrue(contextFile.exists())
        
        val content = contextFile.readText()
        assertTrue(content.contains("Test Task"))
        assertTrue(content.contains("Test Description"))
        assertTrue(content.contains("Test Project"))
        assertTrue(content.contains("feature/test"))
    }
    
    @Test
    fun `should handle task context without optional fields`() = runTest {
        // Given
        val workspace = tempDir.toFile()
        val taskContext = TaskContext(
            taskId = UUID.randomUUID(),
            title = "Test Task",
            description = null,
            projectName = "Test Project",
            branchName = null
        )
        
        // When
        ideService.launchIDE(
            ideType = IDEType.CURSOR,
            workspacePath = workspace.absolutePath,
            taskContext = taskContext
        )
        
        // Then
        val contextFile = File(workspace, "TASK_CONTEXT.md")
        assertTrue(contextFile.exists())
        
        val content = contextFile.readText()
        assertTrue(content.contains("Test Task"))
        assertTrue(content.contains("Test Project"))
    }
}

