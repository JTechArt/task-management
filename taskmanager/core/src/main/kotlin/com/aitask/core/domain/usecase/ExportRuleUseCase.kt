package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.repository.RuleRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.UUID

class ExportRuleUseCase(
    private val ruleRepository: RuleRepository
) {
    suspend fun exportAll(includeArchived: Boolean = false): Result<String> {
        return try {
            val rules = ruleRepository.findAll()
            val filtered = if (includeArchived) rules else rules.filter { !it.isArchived }
            
            if (filtered.isEmpty()) {
                return Result.failure(NoRulesToExportException("No rules available to export"))
            }
            
            val exportData = filtered.map { it.toExportData() }
            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(exportData)
            
            Result.success(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportByIds(ruleIds: List<UUID>): Result<String> {
        return try {
            if (ruleIds.isEmpty()) {
                return Result.failure(NoRulesToExportException("No rule IDs provided"))
            }
            
            val rules = ruleIds.mapNotNull { ruleRepository.findById(it) }
            
            if (rules.isEmpty()) {
                return Result.failure(NoRulesToExportException("No valid rules found for export"))
            }
            
            val exportData = rules.map { it.toExportData() }
            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(exportData)
            
            Result.success(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun Rule.toExportData(): RuleExportData {
        return RuleExportData(
            name = name,
            content = content,
            category = category?.name,
            scope = scope.name,
            targetIDE = targetIDE?.name
        )
    }
}

class NoRulesToExportException(message: String) : Exception(message)

