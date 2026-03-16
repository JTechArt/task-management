package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RuleRepository
import com.aitask.core.domain.service.RuleApplicationResult
import com.aitask.core.domain.service.RuleApplicationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ApplyRulesToWorkspaceUseCaseTest {
    
    private lateinit var ruleRepository: RuleRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var ruleApplicationService: RuleApplicationService
    private lateinit var activityRepository: ActivityRepository
    private lateinit var useCase: ApplyRulesToWorkspaceUseCase
    
    @BeforeEach
    fun setup() {
        ruleRepository = mockk()
        projectRepository = mockk()
        ruleApplicationService = mockk()
        activityRepository = mockk()
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        useCase = ApplyRulesToWorkspaceUseCase(
            ruleRepository,
            projectRepository,
            ruleApplicationService,
            activityRepository
        )
    }
    
    @Test
    fun `should apply global and project rules successfully`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val workspacePath = "/workspace/test"
        val ideType = IDEType.CURSOR
        
        val project = createProject(projectId)
        val globalRule = createRule(scope = RuleScope.GLOBAL)
        val projectRule = createRule(scope = RuleScope.PROJECT)
        
        val request = ApplyRulesRequest(
            workspacePath = workspacePath,
            projectId = projectId,
            ideType = ideType
        )
        
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { ruleApplicationService.validateWorkspace(workspacePath) } returns true
        coEvery { ruleRepository.findByScope(RuleScope.GLOBAL) } returns listOf(globalRule)
        coEvery { ruleRepository.findByProject(projectId) } returns listOf(projectRule)
        coEvery { ruleRepository.findByScope(RuleScope.IDE) } returns emptyList()
        coEvery { 
            ruleApplicationService.applyRulesToWorkspace(workspacePath, any(), ideType) 
        } returns Result.success(
            RuleApplicationResult(
                filesCreated = listOf("/workspace/test/.cursor/rules/general-rules.md"),
                rulesApplied = 2,
                success = true
            )
        )
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isSuccess)
        val appliedRules = result.getOrThrow()
        assertEquals(2, appliedRules.totalRulesApplied)
        assertEquals(0, appliedRules.totalRulesSkipped)
        assertTrue(appliedRules.success)
        
        coVerify { ruleApplicationService.applyRulesToWorkspace(workspacePath, any(), ideType) }
    }
    
    @Test
    fun `should skip archived rules`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val workspacePath = "/workspace/test"
        
        val project = createProject(projectId)
        val activeRule = createRule(scope = RuleScope.GLOBAL, archived = false)
        val archivedRule = createRule(scope = RuleScope.GLOBAL, archived = true)
        
        val request = ApplyRulesRequest(
            workspacePath = workspacePath,
            projectId = projectId,
            ideType = null
        )
        
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { ruleApplicationService.validateWorkspace(workspacePath) } returns true
        coEvery { ruleRepository.findByScope(RuleScope.GLOBAL) } returns listOf(activeRule, archivedRule)
        coEvery { ruleRepository.findByProject(projectId) } returns emptyList()
        coEvery { ruleRepository.findByScope(RuleScope.IDE) } returns emptyList()
        coEvery { 
            ruleApplicationService.applyRulesToWorkspace(workspacePath, listOf(activeRule), null) 
        } returns Result.success(
            RuleApplicationResult(
                filesCreated = listOf("/workspace/test/.aitask/rules/general-rules.md"),
                rulesApplied = 1,
                success = true
            )
        )
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isSuccess)
        val appliedRules = result.getOrThrow()
        assertEquals(1, appliedRules.totalRulesApplied)
        assertEquals(1, appliedRules.totalRulesSkipped)
        assertEquals(SkipReason.ARCHIVED, appliedRules.skippedRules[0].reason)
    }
    
    @Test
    fun `should skip rules with IDE mismatch`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val workspacePath = "/workspace/test"
        val ideType = IDEType.CURSOR
        
        val project = createProject(projectId)
        val cursorRule = createRule(scope = RuleScope.IDE, targetIDE = IDEType.CURSOR)
        val vscodeRule = createRule(scope = RuleScope.IDE, targetIDE = IDEType.VS_CODE)
        
        val request = ApplyRulesRequest(
            workspacePath = workspacePath,
            projectId = projectId,
            ideType = ideType
        )
        
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { ruleApplicationService.validateWorkspace(workspacePath) } returns true
        coEvery { ruleRepository.findByScope(RuleScope.GLOBAL) } returns emptyList()
        coEvery { ruleRepository.findByProject(projectId) } returns listOf(vscodeRule)
        coEvery { ruleRepository.findByScope(RuleScope.IDE) } returns listOf(cursorRule)
        coEvery { 
            ruleApplicationService.applyRulesToWorkspace(workspacePath, listOf(cursorRule), ideType) 
        } returns Result.success(
            RuleApplicationResult(
                filesCreated = listOf("/workspace/test/.cursor/rules/general-rules.md"),
                rulesApplied = 1,
                success = true
            )
        )
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isSuccess)
        val appliedRules = result.getOrThrow()
        assertEquals(1, appliedRules.totalRulesApplied)
        assertEquals(1, appliedRules.totalRulesSkipped)
        assertEquals(SkipReason.IDE_MISMATCH, appliedRules.skippedRules[0].reason)
    }

    @Test
    fun `should fail when project not found`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val request = ApplyRulesRequest(
            workspacePath = "/workspace/test",
            projectId = projectId,
            ideType = null
        )

        coEvery { projectRepository.findById(projectId) } returns null

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProjectNotFoundException)
    }

    @Test
    fun `should fail when workspace is invalid`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val workspacePath = "/invalid/workspace"
        val project = createProject(projectId)

        val request = ApplyRulesRequest(
            workspacePath = workspacePath,
            projectId = projectId,
            ideType = null
        )

        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { ruleApplicationService.validateWorkspace(workspacePath) } returns false

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuleApplicationException)
    }

    @Test
    fun `should handle empty rules gracefully`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val workspacePath = "/workspace/test"
        val project = createProject(projectId)

        val request = ApplyRulesRequest(
            workspacePath = workspacePath,
            projectId = projectId,
            ideType = null
        )

        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { ruleApplicationService.validateWorkspace(workspacePath) } returns true
        coEvery { ruleRepository.findByScope(RuleScope.GLOBAL) } returns emptyList()
        coEvery { ruleRepository.findByProject(projectId) } returns emptyList()
        coEvery { ruleRepository.findByScope(RuleScope.IDE) } returns emptyList()

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        val appliedRules = result.getOrThrow()
        assertEquals(0, appliedRules.totalRulesApplied)
        assertEquals(0, appliedRules.totalRulesSkipped)
        assertTrue(appliedRules.success)
    }

    // Helper methods
    private fun createProject(id: UUID) = Project(
        id = id,
        name = "Test Project",
        description = null,
        workspacePath = "/workspace",
        branchTemplate = "task-{taskId}",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        archivedAt = null
    )

    private fun createRule(
        scope: RuleScope,
        targetIDE: IDEType? = null,
        archived: Boolean = false
    ) = Rule(
        id = UUID.randomUUID(),
        name = "Test Rule",
        content = "Test content",
        category = RuleCategory.CODING_STANDARDS,
        scope = scope,
        targetIDE = targetIDE,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        archivedAt = if (archived) Instant.now() else null
    )
}
