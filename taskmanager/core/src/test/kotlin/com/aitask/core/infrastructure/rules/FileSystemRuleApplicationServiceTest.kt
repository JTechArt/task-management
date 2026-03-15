package com.aitask.core.infrastructure.rules

import com.aitask.core.domain.model.IDEType
import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.RuleCategory
import com.aitask.core.domain.model.RuleScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

class FileSystemRuleApplicationServiceTest {
    
    private lateinit var service: FileSystemRuleApplicationService
    
    @TempDir
    lateinit var tempDir: Path
    
    @BeforeEach
    fun setup() {
        service = FileSystemRuleApplicationService()
    }
    
    @Test
    fun `should apply rules to Cursor workspace`() = runTest {
        // Given
        val workspace = tempDir.toFile()
        val rules = listOf(
            createRule(name = "Coding Standards", category = RuleCategory.CODING_STANDARDS),
            createRule(name = "Architecture", category = RuleCategory.ARCHITECTURE)
        )
        
        // When
        val result = service.applyRulesToWorkspace(
            workspacePath = workspace.absolutePath,
            rules = rules,
            ideType = IDEType.CURSOR
        )
        
        // Then
        assertTrue(result.isSuccess)
        val applicationResult = result.getOrThrow()
        assertEquals(2, applicationResult.rulesApplied)
        assertEquals(2, applicationResult.filesCreated.size)
        assertTrue(applicationResult.success)
        
        // Verify files were created
        val cursorDir = File(workspace, ".cursor/rules")
        assertTrue(cursorDir.exists())
        assertTrue(File(cursorDir, "coding-standards.md").exists())
        assertTrue(File(cursorDir, "architecture.md").exists())
    }
    
    @Test
    fun `should apply rules to VS Code workspace`() = runTest {
        // Given
        val workspace = tempDir.toFile()
        val rules = listOf(createRule(name = "Test Rule", category = RuleCategory.TESTING))
        
        // When
        val result = service.applyRulesToWorkspace(
            workspacePath = workspace.absolutePath,
            rules = rules,
            ideType = IDEType.VS_CODE
        )
        
        // Then
        assertTrue(result.isSuccess)
        
        // Verify files were created in VS Code directory
        val vscodeDir = File(workspace, ".vscode/rules")
        assertTrue(vscodeDir.exists())
        assertTrue(File(vscodeDir, "testing.md").exists())
    }
    
    @Test
    fun `should apply rules to IntelliJ workspace`() = runTest {
        // Given
        val workspace = tempDir.toFile()
        val rules = listOf(createRule(name = "Test Rule", category = RuleCategory.TESTING))
        
        // When
        val result = service.applyRulesToWorkspace(
            workspacePath = workspace.absolutePath,
            rules = rules,
            ideType = IDEType.INTELLIJ_IDEA
        )
        
        // Then
        assertTrue(result.isSuccess)
        
        // Verify files were created in IntelliJ directory
        val ideaDir = File(workspace, ".idea/rules")
        assertTrue(ideaDir.exists())
        assertTrue(File(ideaDir, "testing.md").exists())
    }
    
    @Test
    fun `should apply rules to generic workspace when IDE type is null`() = runTest {
        // Given
        val workspace = tempDir.toFile()
        val rules = listOf(createRule(name = "Test Rule", category = null))
        
        // When
        val result = service.applyRulesToWorkspace(
            workspacePath = workspace.absolutePath,
            rules = rules,
            ideType = null
        )
        
        // Then
        assertTrue(result.isSuccess)
        
        // Verify files were created in generic directory
        val aitaskDir = File(workspace, ".aitask/rules")
        assertTrue(aitaskDir.exists())
        assertTrue(File(aitaskDir, "general-rules.md").exists())
    }
    
    @Test
    fun `should group rules by category`() = runTest {
        // Given
        val workspace = tempDir.toFile()
        val rules = listOf(
            createRule(name = "Rule 1", category = RuleCategory.CODING_STANDARDS),
            createRule(name = "Rule 2", category = RuleCategory.CODING_STANDARDS),
            createRule(name = "Rule 3", category = RuleCategory.TESTING)
        )
        
        // When
        val result = service.applyRulesToWorkspace(
            workspacePath = workspace.absolutePath,
            rules = rules,
            ideType = IDEType.CURSOR
        )
        
        // Then
        assertTrue(result.isSuccess)
        val applicationResult = result.getOrThrow()
        assertEquals(3, applicationResult.rulesApplied)
        assertEquals(2, applicationResult.filesCreated.size) // 2 categories
        
        // Verify file contents
        val cursorDir = File(workspace, ".cursor/rules")
        val codingStandardsFile = File(cursorDir, "coding-standards.md")
        val testingFile = File(cursorDir, "testing.md")
        
        assertTrue(codingStandardsFile.exists())
        assertTrue(testingFile.exists())
        
        val codingStandardsContent = codingStandardsFile.readText()
        assertTrue(codingStandardsContent.contains("Rule 1"))
        assertTrue(codingStandardsContent.contains("Rule 2"))
        
        val testingContent = testingFile.readText()
        assertTrue(testingContent.contains("Rule 3"))
    }
    
    @Test
    fun `should validate workspace exists`() = runTest {
        // Given
        val validWorkspace = tempDir.toFile().absolutePath
        val invalidWorkspace = "/nonexistent/workspace"
        
        // When/Then
        assertTrue(service.validateWorkspace(validWorkspace))
        assertFalse(service.validateWorkspace(invalidWorkspace))
    }
    
    // Helper methods
    private fun createRule(
        name: String,
        category: RuleCategory?
    ) = Rule(
        id = UUID.randomUUID(),
        name = name,
        content = "Test content for $name",
        category = category,
        scope = RuleScope.GLOBAL,
        targetIDE = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}

