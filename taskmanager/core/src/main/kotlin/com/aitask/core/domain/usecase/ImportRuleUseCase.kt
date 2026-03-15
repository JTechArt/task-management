package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.CreateRuleRequest
import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.RuleExport
import com.aitask.core.domain.repository.RuleRepository
import com.aitask.core.domain.validation.RuleValidator
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class RuleExportData(
    val name: String,
    val content: String,
    val category: String?,
    val scope: String,
    val targetIDE: String?
)

class ImportRuleUseCase(
    private val ruleRepository: RuleRepository,
    private val ruleValidator: RuleValidator
) {
    suspend operator fun invoke(jsonContent: String): Result<List<Rule>> {
        return try {
            // Parse JSON
            val json = Json { ignoreUnknownKeys = true }
            val exportData = try {
                json.decodeFromString<List<RuleExportData>>(jsonContent)
            } catch (e: Exception) {
                return Result.failure(InvalidImportFormatException("Invalid JSON format: ${e.message}"))
            }
            
            // Import each rule
            val importedRules = mutableListOf<Rule>()
            for (data in exportData) {
                try {
                    val request = CreateRuleRequest(
                        name = data.name,
                        content = data.content,
                        category = data.category?.let { 
                            try {
                                com.aitask.core.domain.model.RuleCategory.valueOf(it)
                            } catch (e: Exception) {
                                null
                            }
                        },
                        scope = try {
                            com.aitask.core.domain.model.RuleScope.valueOf(data.scope)
                        } catch (e: Exception) {
                            com.aitask.core.domain.model.RuleScope.GLOBAL
                        },
                        targetIDE = data.targetIDE?.let {
                            try {
                                com.aitask.core.domain.model.IDEType.valueOf(it)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    )
                    
                    // Validate
                    val validation = ruleValidator.validate(request)
                    if (!validation.isValid) {
                        continue // Skip invalid rules
                    }
                    
                    // Create rule
                    val now = Instant.now()
                    val rule = Rule(
                        id = UUID.randomUUID(),
                        name = request.name,
                        content = request.content,
                        category = request.category,
                        scope = request.scope,
                        targetIDE = request.targetIDE,
                        createdAt = now,
                        updatedAt = now
                    )
                    
                    val created = ruleRepository.create(rule)
                    importedRules.add(created)
                } catch (e: Exception) {
                    // Skip rules that fail to import
                    continue
                }
            }
            
            if (importedRules.isEmpty()) {
                return Result.failure(NoValidRulesException("No valid rules found in import data"))
            }
            
            Result.success(importedRules)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class InvalidImportFormatException(message: String) : Exception(message)
class NoValidRulesException(message: String) : Exception(message)

