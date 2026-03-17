package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.DataExportBundle
import com.aitask.core.domain.model.GitProvider
import com.aitask.core.domain.model.IDEType
import com.aitask.core.domain.model.ImportConflict
import com.aitask.core.domain.model.ImportResult
import com.aitask.core.domain.model.ImportValidationResult
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Repository
import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.RuleCategory
import com.aitask.core.domain.model.RuleScope
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.model.TaskType
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.RuleRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.RetentionPolicy
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

/**
 * Import projects, tasks, repositories, and rules from a portable JSON export.
 * AC3: Validates and reports conflicts before applying.
 * AC5: Returns clear summary of processed records.
 */
class ImportDataUseCase(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val repositoryRepository: RepositoryRepository,
    private val ruleRepository: RuleRepository
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val requiredSchemaVersion = 1

    suspend fun validate(jsonContent: String): Result<ImportValidationResult> {
        return try {
            val bundle = json.decodeFromString(DataExportBundle.serializer(), jsonContent)
            if (bundle.schemaVersion != requiredSchemaVersion) {
                return Result.failure(
                    IllegalArgumentException("Unsupported schema version: ${bundle.schemaVersion}, required: $requiredSchemaVersion")
                )
            }
            val existingProjects = projectRepository.findAll()
            val existingNames = existingProjects.map { it.name }.toSet()
            val existingWorkspacePaths = existingProjects.map { it.workspacePath }.toSet()
            val conflicts = mutableListOf<ImportConflict>()
            for (p in bundle.projects) {
                if (existingNames.contains(p.name)) {
                    conflicts.add(ImportConflict("Project", p.name, "Project name already exists: ${p.name}"))
                }
                if (existingWorkspacePaths.contains(p.workspacePath)) {
                    conflicts.add(ImportConflict("Project", p.workspacePath, "Workspace path already in use: ${p.workspacePath}"))
                }
            }
            Result.success(ImportValidationResult(isValid = conflicts.isEmpty(), conflicts = conflicts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun apply(jsonContent: String): Result<ImportResult> {
        return try {
            val bundle = json.decodeFromString(DataExportBundle.serializer(), jsonContent)
            if (bundle.schemaVersion != requiredSchemaVersion) {
                return Result.failure(
                    IllegalArgumentException("Unsupported schema version: ${bundle.schemaVersion}, required: $requiredSchemaVersion")
                )
            }
            val existingProjects = projectRepository.findAll()
            val existingNames = mutableSetOf<String>().apply { addAll(existingProjects.map { it.name }) }
            val existingWorkspacePaths = mutableSetOf<String>().apply { addAll(existingProjects.map { it.workspacePath }) }
            val projectIdMap = mutableMapOf<String, UUID>()
            val ruleIdMap = mutableMapOf<String, UUID>()
            val conflicts = mutableListOf<ImportConflict>()
            val errors = mutableListOf<String>()
            var projectsCreated = 0
            var tasksCreated = 0
            var reposCreated = 0
            var rulesCreated = 0
            var projectRulesLinked = 0
            val now = Instant.now()
            for (p in bundle.projects) {
                val name = disambiguateName(p.name, existingNames)
                val workspacePath = disambiguatePath(p.workspacePath, existingWorkspacePaths)
                val retentionPolicy = parseRetentionPolicy(p.retentionPolicy)
                val project = Project(
                    id = UUID.randomUUID(),
                    name = name,
                    description = p.description,
                    workspacePath = workspacePath,
                    branchTemplate = p.branchTemplate,
                    retentionPolicy = retentionPolicy,
                    tags = p.tags,
                    team = p.team,
                    createdAt = now,
                    updatedAt = now,
                    archivedAt = p.archivedAt?.let { parseInstant(it) }
                )
                projectRepository.create(project)
                projectIdMap[p.id] = project.id
                existingNames.add(project.name)
                existingWorkspacePaths.add(project.workspacePath)
                projectsCreated++
            }
            for (r in bundle.rules) {
                val ruleScope = parseRuleScope(r.scope)
                val category = r.category?.let { parseRuleCategory(it) }
                val targetIDE = r.targetIDE?.let { parseIDEType(it) }
                val rule = Rule(
                    id = UUID.randomUUID(),
                    name = r.name,
                    content = r.content,
                    category = category,
                    scope = ruleScope,
                    targetIDE = targetIDE,
                    createdAt = now,
                    updatedAt = now,
                    archivedAt = r.archivedAt?.let { parseInstant(it) }
                )
                ruleRepository.create(rule)
                ruleIdMap[r.id] = rule.id
                rulesCreated++
            }
            for (t in bundle.tasks) {
                val newProjectId = projectIdMap[t.projectId] ?: continue
                val taskType = parseTaskType(t.taskType)
                val status = parseTaskStatus(t.status)
                val task = Task(
                    id = UUID.randomUUID(),
                    title = t.title,
                    description = t.description,
                    taskType = taskType,
                    status = status,
                    projectId = newProjectId,
                    workspacePath = t.workspacePath,
                    branchName = t.branchName,
                    workspaceCleanedAt = t.workspaceCleanedAt?.let { parseInstant(it) },
                    createdAt = now,
                    updatedAt = now,
                    completedAt = t.completedAt?.let { parseInstant(it) }
                )
                taskRepository.create(task)
                tasksCreated++
            }
            for (repo in bundle.repositories) {
                val newProjectId = projectIdMap[repo.projectId] ?: continue
                val provider = parseGitProvider(repo.provider)
                val authType = parseAuthType(repo.authType)
                val preferredIDEs = repo.preferredIDEs.mapNotNull { parseIDEType(it) }
                    .ifEmpty { listOf(IDEType.CURSOR) }
                val repository = Repository(
                    id = UUID.randomUUID(),
                    projectId = newProjectId,
                    name = repo.name,
                    cloneUrl = repo.cloneUrl,
                    provider = provider,
                    authType = authType,
                    preferredIDEs = preferredIDEs,
                    isPrimary = repo.isPrimary,
                    createdAt = now,
                    updatedAt = now
                )
                repositoryRepository.create(repository)
                reposCreated++
            }
            for (pr in bundle.projectRules) {
                val newProjectId = projectIdMap[pr.projectId] ?: continue
                val newRuleId = ruleIdMap[pr.ruleId] ?: continue
                try {
                    ruleRepository.attachToProject(newProjectId, newRuleId)
                    projectRulesLinked++
                } catch (e: Exception) {
                    errors.add("Failed to link rule ${pr.ruleId} to project ${pr.projectId}: ${e.message}")
                }
            }
            Result.success(ImportResult(
                projectsCreated = projectsCreated,
                tasksCreated = tasksCreated,
                reposCreated = reposCreated,
                rulesCreated = rulesCreated,
                projectRulesLinked = projectRulesLinked,
                conflicts = conflicts,
                errors = errors
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun disambiguateName(name: String, existing: MutableSet<String>): String {
        var candidate = name
        var suffix = 1
        while (existing.contains(candidate)) {
            candidate = "$name (imported $suffix)"
            suffix++
        }
        return candidate
    }

    private fun disambiguatePath(path: String, existing: MutableSet<String>): String {
        var candidate = path
        var suffix = 1
        while (existing.contains(candidate)) {
            candidate = "$path-imported-$suffix"
            suffix++
        }
        return candidate
    }

    private fun parseInstant(s: String): Instant? = try { Instant.parse(s) } catch (_: Exception) { null }
    private fun parseRetentionPolicy(s: String): RetentionPolicy =
        try { RetentionPolicy.valueOf(s) } catch (_: Exception) { RetentionPolicy.KEEP_ALL }
    private fun parseTaskType(s: String): TaskType = try { TaskType.valueOf(s) } catch (_: Exception) { TaskType.FEATURE }
    private fun parseTaskStatus(s: String): TaskStatus = try { TaskStatus.valueOf(s) } catch (_: Exception) { TaskStatus.PENDING }
    private fun parseGitProvider(s: String): GitProvider = try { GitProvider.valueOf(s) } catch (_: Exception) { GitProvider.OTHER }
    private fun parseAuthType(s: String): com.aitask.core.domain.model.AuthType =
        try { com.aitask.core.domain.model.AuthType.valueOf(s) } catch (_: Exception) { com.aitask.core.domain.model.AuthType.HTTPS }
    private fun parseRuleScope(s: String): RuleScope = try { RuleScope.valueOf(s) } catch (_: Exception) { RuleScope.GLOBAL }
    private fun parseRuleCategory(s: String): RuleCategory? = try { RuleCategory.valueOf(s) } catch (_: Exception) { null }
    private fun parseIDEType(s: String): IDEType? = try { IDEType.valueOf(s) } catch (_: Exception) { null }
}
