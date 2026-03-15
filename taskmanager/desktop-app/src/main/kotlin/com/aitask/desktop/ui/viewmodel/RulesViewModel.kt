package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.model.*
import com.aitask.core.domain.usecase.*
import com.aitask.desktop.di.DependencyContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class RulesViewModel(
    private val getRulesUseCase: GetRulesUseCase = DependencyContainer.getRulesUseCase,
    private val createRuleUseCase: CreateRuleUseCase = DependencyContainer.createRuleUseCase,
    private val updateRuleUseCase: UpdateRuleUseCase = DependencyContainer.updateRuleUseCase,
    private val deleteRuleUseCase: DeleteRuleUseCase = DependencyContainer.deleteRuleUseCase,
    private val attachRuleUseCase: AttachRuleUseCase = DependencyContainer.attachRuleUseCase,
    private val detachRuleUseCase: DetachRuleUseCase = DependencyContainer.detachRuleUseCase,
    private val exportRuleUseCase: ExportRuleUseCase = DependencyContainer.exportRuleUseCase,
    private val importRuleUseCase: ImportRuleUseCase = DependencyContainer.importRuleUseCase,
    private val getProjectsUseCase: com.aitask.core.domain.usecase.GetProjectsUseCase = DependencyContainer.getProjectsUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    var uiState by mutableStateOf(RulesUiState())
        private set
    
    init {
        loadRules()
        loadProjects()
    }
    
    fun loadRules() {
        uiState = uiState.copy(isLoading = true, error = null)
        scope.launch {
            val result = getRulesUseCase.getAll(includeArchived = false)
            result.fold(
                onSuccess = { rules ->
                    uiState = uiState.copy(
                        rules = rules,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load rules"
                    )
                }
            )
        }
    }
    
    private fun loadProjects() {
        scope.launch {
            val result = getProjectsUseCase(includeArchived = false)
            result.fold(
                onSuccess = { projects ->
                    uiState = uiState.copy(projects = projects)
                },
                onFailure = { /* Silently fail - projects will be empty */ }
            )
        }
    }
    
    fun selectRule(rule: Rule) {
        uiState = uiState.copy(selectedRule = rule)
        loadAttachedProjects(rule.id)
    }
    
    private fun loadAttachedProjects(ruleId: UUID) {
        scope.launch {
            val attachedProjectIds = mutableListOf<UUID>()
            for (project in uiState.projects) {
                val result = getRulesUseCase.getByProject(project.id)
                result.fold(
                    onSuccess = { rules ->
                        if (rules.any { it.id == ruleId }) {
                            attachedProjectIds.add(project.id)
                        }
                    },
                    onFailure = { /* Ignore */ }
                )
            }
            uiState = uiState.copy(attachedProjectIds = attachedProjectIds)
        }
    }
    
    fun createRule(
        name: String,
        content: String,
        category: RuleCategory?,
        ruleScope: RuleScope,
        targetIDE: IDEType?
    ) {
        uiState = uiState.copy(isSaving = true, error = null)
        scope.launch {
            val request = CreateRuleRequest(name, content, category, ruleScope, targetIDE)
            val result = createRuleUseCase(request)
            result.fold(
                onSuccess = {
                    uiState = uiState.copy(
                        isSaving = false,
                        showCreateDialog = false
                    )
                    loadRules()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        error = error.message ?: "Failed to create rule"
                    )
                }
            )
        }
    }
    
    fun updateRule(
        id: UUID,
        name: String?,
        content: String?,
        category: RuleCategory?,
        ruleScope: RuleScope?,
        targetIDE: IDEType?
    ) {
        uiState = uiState.copy(isSaving = true, error = null)
        scope.launch {
            val request = UpdateRuleRequest(id, name, content, category, ruleScope, targetIDE)
            val result = updateRuleUseCase(request)
            result.fold(
                onSuccess = {
                    uiState = uiState.copy(
                        isSaving = false,
                        showEditDialog = false
                    )
                    loadRules()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        error = error.message ?: "Failed to update rule"
                    )
                }
            )
        }
    }

    fun deleteRule(ruleId: UUID) {
        scope.launch {
            val result = deleteRuleUseCase(ruleId)
            result.fold(
                onSuccess = {
                    loadRules()
                    if (uiState.selectedRule?.id == ruleId) {
                        uiState = uiState.copy(selectedRule = null)
                    }
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to delete rule"
                    )
                }
            )
        }
    }

    fun attachRule(projectId: UUID, ruleId: UUID) {
        scope.launch {
            val request = AttachRuleRequest(ruleId, projectId)
            val result = attachRuleUseCase(request)
            result.fold(
                onSuccess = {
                    loadAttachedProjects(ruleId)
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to attach rule"
                    )
                }
            )
        }
    }

    fun detachRule(projectId: UUID, ruleId: UUID) {
        scope.launch {
            val request = DetachRuleRequest(ruleId, projectId)
            val result = detachRuleUseCase(request)
            result.fold(
                onSuccess = {
                    loadAttachedProjects(ruleId)
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to detach rule"
                    )
                }
            )
        }
    }

    fun exportRules(ruleIds: List<UUID>) {
        scope.launch {
            val exportResult = if (ruleIds.isEmpty()) {
                exportRuleUseCase.exportAll()
            } else {
                exportRuleUseCase.exportByIds(ruleIds)
            }
            exportResult.fold(
                onSuccess = { json ->
                    copyToClipboard(json)
                    uiState = uiState.copy(exportFeedback = "Copied to clipboard")
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to export rules"
                    )
                }
            )
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        val stringSelection = java.awt.datatransfer.StringSelection(text)
        clipboard.setContents(stringSelection, null)
    }

    fun importRules(jsonContent: String) {
        uiState = uiState.copy(isSaving = true, error = null)
        scope.launch {
            val result = importRuleUseCase(jsonContent)
            result.fold(
                onSuccess = { importedRules ->
                    uiState = uiState.copy(
                        isSaving = false,
                        showImportDialog = false
                    )
                    loadRules()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        error = error.message ?: "Failed to import rules"
                    )
                }
            )
        }
    }

    fun showCreateDialog() {
        uiState = uiState.copy(showCreateDialog = true, error = null)
    }

    fun hideCreateDialog() {
        uiState = uiState.copy(showCreateDialog = false, error = null)
    }

    fun showEditDialog(rule: Rule) {
        uiState = uiState.copy(showEditDialog = true, editingRule = rule, error = null)
    }

    fun hideEditDialog() {
        uiState = uiState.copy(showEditDialog = false, editingRule = null, error = null)
    }

    fun showImportDialog() {
        uiState = uiState.copy(showImportDialog = true, error = null)
    }

    fun hideImportDialog() {
        uiState = uiState.copy(showImportDialog = false, error = null)
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    fun clearExportFeedback() {
        uiState = uiState.copy(exportFeedback = null)
    }
}

data class RulesUiState(
    val rules: List<Rule> = emptyList(),
    val projects: List<Project> = emptyList(),
    val selectedRule: Rule? = null,
    val attachedProjectIds: List<UUID> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val editingRule: Rule? = null,
    val error: String? = null,
    val exportFeedback: String? = null
)

