package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.DataExportBundle
import com.aitask.core.domain.model.GitProvider
import com.aitask.core.domain.model.IDEType
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.ProjectExportItem
import com.aitask.core.domain.model.ProjectRule
import com.aitask.core.domain.model.ProjectRuleExportItem
import com.aitask.core.domain.model.Repository
import com.aitask.core.domain.model.RepositoryExportItem
import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.RuleCategory
import com.aitask.core.domain.model.RuleExportItem
import com.aitask.core.domain.model.RuleScope
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskExportItem
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.RuleRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.util.CredentialRedactor
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Export projects, tasks, repositories, and rules into a portable JSON format.
 * Credentials excluded per Export Security (architecture).
 */
class ExportDataUseCase(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val repositoryRepository: RepositoryRepository,
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(includeArchived: Boolean = false): Result<String> {
        return try {
            val projects = if (includeArchived) {
                projectRepository.findAll()
            } else {
                projectRepository.findAllActive()
            }
            val projectIds = projects.map { it.id }.toSet()
            val tasks = projectIds.flatMap { taskRepository.findByProject(it) }
            val repositories = projectIds.flatMap { repositoryRepository.findByProject(it) }
            val rules = ruleRepository.findAll()
            val rulesFiltered = if (includeArchived) rules else rules.filter { !it.isArchived }
            val projectRules = projectIds.flatMap { ruleRepository.findProjectRules(it) }
            val bundle = DataExportBundle(
                exportedAt = Instant.now().toString(),
                projects = projects.map { it.toExportItem() },
                tasks = tasks.map { it.toExportItem() },
                repositories = repositories.map { it.toExportItem() },
                rules = rulesFiltered.map { it.toExportItem() },
                projectRules = projectRules.map { ProjectRuleExportItem(
                    projectId = it.projectId.toString(),
                    ruleId = it.ruleId.toString()
                ) }.filter { pr ->
                    projectIds.contains(java.util.UUID.fromString(pr.projectId)) &&
                        rulesFiltered.any { r -> r.id.toString() == pr.ruleId }
                }
            )
            val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
            Result.success(json.encodeToString(DataExportBundle.serializer(), bundle))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun Project.toExportItem(): ProjectExportItem = ProjectExportItem(
    id = id.toString(),
    name = name,
    description = description,
    workspacePath = workspacePath,
    branchTemplate = branchTemplate,
    retentionPolicy = retentionPolicy.name,
    tags = tags,
    team = team,
    archivedAt = archivedAt?.toString()
)

private fun Task.toExportItem(): TaskExportItem = TaskExportItem(
    id = id.toString(),
    projectId = projectId.toString(),
    title = title,
    description = description,
    taskType = taskType.name,
    status = status.name,
    workspacePath = workspacePath,
    branchName = branchName,
    workspaceCleanedAt = workspaceCleanedAt?.toString(),
    completedAt = completedAt?.toString()
)

private fun Repository.toExportItem(): RepositoryExportItem = RepositoryExportItem(
    id = id.toString(),
    projectId = projectId.toString(),
    name = name,
    cloneUrl = CredentialRedactor.redactUrl(cloneUrl),
    provider = provider.name,
    authType = authType.name,
    preferredIDEs = preferredIDEs.map { it.name },
    isPrimary = isPrimary
)

private fun Rule.toExportItem(): RuleExportItem = RuleExportItem(
    id = id.toString(),
    name = name,
    content = content,
    category = category?.name,
    scope = scope.name,
    targetIDE = targetIDE?.name,
    archivedAt = archivedAt?.toString()
)
