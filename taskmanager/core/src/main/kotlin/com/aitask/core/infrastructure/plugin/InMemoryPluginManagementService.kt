package com.aitask.core.infrastructure.plugin

import com.aitask.core.data.repository.InMemoryPluginConfigurationRepository
import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.HealthState
import com.aitask.core.domain.plugin.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.PluginConfigurationRepository
import com.aitask.core.domain.service.PluginManagementService
import com.aitask.core.domain.service.PluginPrerequisiteProbe
import java.time.Instant
import java.util.UUID

private const val PLUGIN_ENTITY_TYPE = "plugin"

class InMemoryPluginManagementService(
    private val activityRepository: ActivityRepository,
    private val configurationRepository: PluginConfigurationRepository = InMemoryPluginConfigurationRepository(),
    private val prerequisiteProbe: PluginPrerequisiteProbe = NoOpPluginPrerequisiteProbe(),
    private val host: PluginHost = InMemoryPluginHost(PluginContractVersion(1, 0)),
    seeds: List<PluginSeed> = PluginCatalogFixtures.defaultSeeds()
) : PluginManagementService {
    private data class PluginRuntimeState(
        val definition: PluginDefinition,
        var installed: Boolean = false,
        var attached: Boolean = false,
        var enabled: Boolean = false,
        var lastActionMessage: String? = null
    )

    private val plugins = seeds.associate { seed ->
        if (seed.installed) {
            host.register(seed.definition)
        }
        seed.definition.manifest.id to PluginRuntimeState(
            definition = seed.definition,
            installed = seed.installed
        )
    }.toMutableMap()

    override suspend fun catalog(): List<PluginCatalogItem> =
        plugins.values
            .map { it.toCatalogItem() }
            .sortedWith(compareBy<PluginCatalogItem> { !it.installed }.thenBy { it.name.lowercase() })

    override suspend fun install(pluginId: String): Result<PluginCatalogItem> =
        performAction(pluginId, ActivityType.PLUGIN_INSTALLED) { runtime ->
            if (runtime.installed) {
                runtime.lastActionMessage = "Plugin already installed"
                runtime.toCatalogItem()
            } else {
                host.register(runtime.definition)
                runtime.installed = true
                runtime.attached = false
                runtime.enabled = false
                runtime.lastActionMessage = "Plugin installed"
                runtime.toCatalogItem()
            }
        }

    override suspend fun attach(pluginId: String): Result<PluginCatalogItem> =
        performAction(pluginId, ActivityType.PLUGIN_ATTACHED) { runtime ->
            ensureInstalled(runtime, "attach")
            if (runtime.attached) {
                runtime.lastActionMessage = "Plugin already attached"
                runtime.toCatalogItem()
            } else {
                val report = host.initialize(pluginId)
                ensureSuccessfulLifecycle(report, "attach")
                runtime.attached = true
                runtime.lastActionMessage = "Plugin attached"
                runtime.toCatalogItem(report)
            }
        }

    override suspend fun enable(pluginId: String): Result<PluginCatalogItem> =
        performAction(pluginId, ActivityType.PLUGIN_ENABLED) { runtime ->
            ensureInstalled(runtime, "enable")
            ensureAttached(runtime, "enable")
            ensureConfigurationReady(runtime.definition)
            if (runtime.enabled) {
                runtime.lastActionMessage = "Plugin already enabled"
                runtime.toCatalogItem()
            } else {
                val report = host.enable(pluginId)
                ensureSuccessfulLifecycle(report, "enable")
                runtime.enabled = true
                runtime.lastActionMessage = "Plugin enabled"
                runtime.toCatalogItem(report)
            }
        }

    override suspend fun getConfiguration(
        pluginId: String,
        scope: PluginConfigurationScope,
        scopeKey: String?
    ): PluginConfigurationSnapshot? {
        val runtime = plugins[pluginId] ?: return null
        return configurationRepository.find(pluginId, scope, scopeKey) ?: defaultConfigurationSnapshot(runtime.definition, scope, scopeKey)
    }

    override suspend fun saveConfiguration(snapshot: PluginConfigurationSnapshot): Result<PluginConfigurationSnapshot> {
        val runtime = plugins[snapshot.pluginId] ?: return Result.failure(
            IllegalArgumentException("Plugin '${snapshot.pluginId}' is not supported")
        )
        val validation = validateConfiguration(snapshot).fold(
            onSuccess = { it },
            onFailure = { return Result.failure(it) }
        )
        if (!validation.valid) {
            return Result.failure(PluginConfigurationValidationException(validation))
        }
        val normalized = snapshot.copy(
            validationStatus = PluginConfigurationValidationStatus.VALID,
            validationMessage = validation.message ?: "Configuration is valid",
            lastValidatedAtEpochMillis = Instant.now().toEpochMilli()
        )
        configurationRepository.save(normalized)
        return Result.success(normalized)
    }

    override suspend fun validateConfiguration(snapshot: PluginConfigurationSnapshot): Result<PluginConfigurationValidationResult> {
        val runtime = plugins[snapshot.pluginId] ?: return Result.failure(
            IllegalArgumentException("Plugin '${snapshot.pluginId}' is not supported")
        )
        val validation = evaluateConfiguration(runtime.definition, snapshot)
        recordActivity(
            type = ActivityType.PLUGIN_VALIDATED,
            plugin = runtime.definition,
            description = "Validated configuration for ${runtime.definition.manifest.name}",
            status = if (validation.valid) ActivityStatus.SUCCESS else ActivityStatus.FAILED,
            metadata = mapOf(
                "pluginId" to snapshot.pluginId,
                "scope" to snapshot.scope.name,
                "scopeKey" to (snapshot.scopeKey ?: ""),
                "valid" to validation.valid.toString(),
                "message" to (validation.message ?: "Configuration validation completed")
            )
        )
        return Result.success(validation)
    }

    override suspend fun validateConfiguration(
        pluginId: String,
        scope: PluginConfigurationScope,
        scopeKey: String?
    ): Result<PluginConfigurationValidationResult> {
        val runtime = plugins[pluginId] ?: return Result.failure(
            IllegalArgumentException("Plugin '$pluginId' is not supported")
        )
        val snapshot = getConfiguration(pluginId, scope, scopeKey)
            ?: defaultConfigurationSnapshot(runtime.definition, scope, scopeKey)
        return validateConfiguration(snapshot).mapCatching { validation ->
            validation.copy(
                lastKnownGoodSnapshot = validation.lastKnownGoodSnapshot
                ?: latestKnownGoodSnapshot(runtime.definition.manifest.id, scope, scopeKey)
            )
        }
    }

    override suspend fun health(pluginId: String): Result<PluginHealthReport> {
        val runtime = plugins[pluginId] ?: return recordCheckFailure(
            activityType = ActivityType.PLUGIN_HEALTH_CHECKED,
            pluginId = pluginId,
            message = "Plugin '$pluginId' is not supported"
        )
        return try {
            val report = host.health(pluginId)
            recordActivity(
                type = ActivityType.PLUGIN_HEALTH_CHECKED,
                plugin = runtime.definition,
                description = "Checked health for ${runtime.definition.manifest.name}",
                status = if (report.state == HealthState.HEALTHY) ActivityStatus.SUCCESS else ActivityStatus.FAILED,
                metadata = mapOf(
                    "pluginId" to pluginId,
                    "healthState" to report.state.name,
                    "message" to report.message
                )
            )
            Result.success(report)
        } catch (e: Exception) {
            recordCheckFailure(
                activityType = ActivityType.PLUGIN_HEALTH_CHECKED,
                pluginId = pluginId,
                message = e.message ?: "Plugin health check failed",
                throwable = e
            )
        }
    }

    override suspend fun recentActivity(pluginId: String, limit: Int): List<Activity> {
        val runtime = plugins[pluginId] ?: return emptyList()
        return activityRepository.findByEntity(PLUGIN_ENTITY_TYPE, entityId(runtime.definition.manifest.id))
            .take(limit)
    }

    override suspend fun disable(pluginId: String): Result<PluginCatalogItem> =
        performAction(pluginId, ActivityType.PLUGIN_DISABLED) { runtime ->
            ensureInstalled(runtime, "disable")
            ensureAttached(runtime, "disable")
            if (!runtime.enabled) {
                runtime.lastActionMessage = "Plugin already disabled"
                runtime.toCatalogItem()
            } else {
                val report = host.disable(pluginId)
                ensureSuccessfulLifecycle(report, "disable")
                runtime.enabled = false
                runtime.lastActionMessage = "Plugin disabled"
                runtime.toCatalogItem(report)
            }
        }

    override suspend fun detach(pluginId: String): Result<PluginCatalogItem> =
        performAction(pluginId, ActivityType.PLUGIN_DETACHED) { runtime ->
            ensureInstalled(runtime, "detach")
            if (!runtime.attached) {
                runtime.lastActionMessage = "Plugin already detached"
                runtime.toCatalogItem()
            } else {
                if (runtime.enabled) {
                    val disableReport = host.disable(pluginId)
                    ensureSuccessfulLifecycle(disableReport, "detach")
                    runtime.enabled = false
                }
                runtime.attached = false
                runtime.lastActionMessage = "Plugin detached"
                runtime.toCatalogItem()
            }
        }

    override suspend fun remove(pluginId: String): Result<PluginCatalogItem> =
        performAction(pluginId, ActivityType.PLUGIN_REMOVED) { runtime ->
            ensureInstalled(runtime, "remove")
            val report = host.remove(pluginId)
            ensureSuccessfulLifecycle(report, "remove")
            runtime.installed = false
            runtime.attached = false
            runtime.enabled = false
            runtime.lastActionMessage = "Plugin removed"
            runtime.toCatalogItem(report)
        }

    private suspend fun ensureConfigurationReady(definition: PluginDefinition) {
        val validation = evaluateConfiguration(
            definition = definition,
            snapshot = getConfiguration(
                pluginId = definition.manifest.id,
                scope = definition.manifest.configurationScope,
                scopeKey = null
            ) ?: defaultConfigurationSnapshot(
                definition = definition,
                scope = definition.manifest.configurationScope,
                scopeKey = null
            )
        )
        if (!validation.valid) {
            throw PluginConfigurationValidationException(validation)
        }
    }

    private fun recordCheckFailure(
        activityType: ActivityType,
        pluginId: String,
        message: String,
        throwable: Throwable? = null
    ): Result<Nothing> {
        val runtime = plugins[pluginId]
        val definition = runtime?.definition ?: UnsupportedPluginDefinition(pluginId)
        if (runtime != null) {
            runtime.lastActionMessage = message
        }
        runBlockingRecordActivity(
            type = activityType,
            plugin = definition,
            description = buildDescription(activityType, definition.manifest.name),
            status = ActivityStatus.FAILED,
            metadata = mapOf(
                "pluginId" to pluginId,
                "error" to message,
                "detail" to (throwable?.message ?: message)
            )
        )
        return Result.failure(IllegalStateException(message, throwable))
    }

    private suspend fun evaluateConfiguration(
        definition: PluginDefinition,
        snapshot: PluginConfigurationSnapshot
    ): PluginConfigurationValidationResult {
        val manifest = definition.manifest
        if (snapshot.scope != manifest.configurationScope) {
            return PluginConfigurationValidationResult(
                valid = false,
                message = "Configuration scope '${snapshot.scope}' does not match plugin scope '${manifest.configurationScope}'",
                lastKnownGoodSnapshot = latestKnownGoodSnapshot(manifest.id, snapshot.scope, snapshot.scopeKey)
            )
        }
        if (manifest.configurationScope == PluginConfigurationScope.PROJECT && snapshot.scopeKey.isNullOrBlank()) {
            return PluginConfigurationValidationResult(
                valid = false,
                message = "Project-scoped plugin '${manifest.name}' requires a project scope key",
                lastKnownGoodSnapshot = latestKnownGoodSnapshot(manifest.id, snapshot.scope, snapshot.scopeKey)
            )
        }

        val fieldValues = mutableMapOf<String, String>().apply {
            putAll(snapshot.fields)
            putAll(snapshot.secrets)
            snapshot.schedule?.let { put("schedule", it) }
        }
        val fieldErrors = linkedMapOf<String, String>()
        val prerequisiteResults = mutableListOf<PluginPrerequisiteResult>()
        val ruleMessages = mutableListOf<String>()

        manifest.configurationSchema.fields.forEach { field ->
            val value = when (field.type) {
                PluginConfigurationFieldType.SECRET -> snapshot.secrets[field.id]
                PluginConfigurationFieldType.SCHEDULE -> snapshot.fields[field.id] ?: snapshot.schedule
                else -> snapshot.fields[field.id]
            }
            if (field.required && value.isNullOrBlank()) {
                fieldErrors[field.id] = "${field.label} is required"
            }
        }

        manifest.configurationSchema.validationRules.forEach { rule ->
            if (rule.whenFieldId != null) {
                if (rule.whenFieldMatches.isEmpty()) {
                    return@forEach
                }
                val trigger = fieldValues[rule.whenFieldId]
                if (trigger == null || trigger !in rule.whenFieldMatches) {
                    return@forEach
                }
            }
            rule.requiredFields.forEach { fieldId ->
                val value = fieldValues[fieldId]
                if (value.isNullOrBlank()) {
                    fieldErrors.putIfAbsent(fieldId, "Required by validation rule '${rule.description}'")
                }
            }
            rule.requiredPrerequisites.forEach { prerequisite ->
                prerequisiteResults += prerequisiteProbe.evaluate(prerequisite, fieldValues)
            }
            if (rule.requiredFields.isNotEmpty() || rule.requiredPrerequisites.isNotEmpty()) {
                val missingFields = rule.requiredFields.filter { fieldValues[it].isNullOrBlank() }
                val missingPrerequisites = rule.requiredPrerequisites
                    .zip(prerequisiteResults.takeLast(rule.requiredPrerequisites.size))
                    .filterNot { it.second.satisfied }
                    .map { it.first.name }
                if (missingFields.isNotEmpty() || missingPrerequisites.isNotEmpty()) {
                    ruleMessages += buildString {
                        append(rule.description)
                        if (missingFields.isNotEmpty()) {
                            append(": missing fields ${missingFields.joinToString()}")
                        }
                        if (missingPrerequisites.isNotEmpty()) {
                            if (missingFields.isNotEmpty()) append("; ")
                            append("missing prerequisites ${missingPrerequisites.joinToString()}")
                        }
                    }
                }
            }
        }

        if (manifest.id == "plugin.slack-channel-analyzer") {
            val mode = fieldValues["schedule_mode"]
            if (mode == "daily" || mode == "manual_and_daily") {
                val time = fieldValues["daily_run_time"]?.trim()
                if (!time.isNullOrBlank() && !time.matches(Regex("""^(?:[01]?\d|2[0-3]):[0-5]\d$"""))) {
                    fieldErrors["daily_run_time"] = "Use HH:mm (24h), e.g. 10:00"
                }
            }
        }

        val schemaPrerequisites = manifest.configurationSchema.prerequisites.map { prerequisite ->
            prerequisiteProbe.evaluate(prerequisite, fieldValues)
        }
        prerequisiteResults += schemaPrerequisites

        val failedPrerequisites = prerequisiteResults.filterNot { it.satisfied }
        val missingFieldMessage = if (fieldErrors.isNotEmpty()) {
            "Missing required plugin fields: ${fieldErrors.values.joinToString()}"
        } else {
            null
        }
        val prerequisiteMessage = if (failedPrerequisites.isNotEmpty()) {
            "Unsatisfied prerequisites: ${failedPrerequisites.joinToString { it.prerequisite.name }}"
        } else {
            null
        }

        val valid = fieldErrors.isEmpty() && failedPrerequisites.isEmpty()
        val message = when {
            valid -> "Configuration is valid"
            missingFieldMessage != null && prerequisiteMessage != null -> "$missingFieldMessage; $prerequisiteMessage"
            missingFieldMessage != null -> missingFieldMessage
            prerequisiteMessage != null -> prerequisiteMessage
            else -> "Plugin configuration is invalid"
        }

        return PluginConfigurationValidationResult(
            valid = valid,
            message = message,
            fieldErrors = fieldErrors,
            prerequisiteResults = prerequisiteResults.distinctBy {
                "${it.prerequisite.type}:${it.prerequisite.name}:${it.prerequisite.value}"
            },
            ruleMessages = ruleMessages,
            lastKnownGoodSnapshot = latestKnownGoodSnapshot(manifest.id, snapshot.scope, snapshot.scopeKey)
        )
    }

    private suspend fun latestKnownGoodSnapshot(
        pluginId: String,
        scope: PluginConfigurationScope,
        scopeKey: String?
    ): PluginConfigurationSnapshot? =
        configurationRepository.find(pluginId, scope, scopeKey)?.takeIf {
            it.validationStatus == PluginConfigurationValidationStatus.VALID
        }

    private fun defaultConfigurationSnapshot(
        definition: PluginDefinition,
        scope: PluginConfigurationScope,
        scopeKey: String?
    ): PluginConfigurationSnapshot = PluginConfigurationSnapshot(
        pluginId = definition.manifest.id,
        scope = scope,
        scopeKey = scopeKey,
        fields = emptyMap(),
        secrets = emptyMap(),
        schedule = null
    )

    private suspend fun performAction(
        pluginId: String,
        activityType: ActivityType,
        action: suspend (PluginRuntimeState) -> PluginCatalogItem
    ): Result<PluginCatalogItem> {
        val runtime = plugins[pluginId] ?: return recordFailure(
            activityType = activityType,
            pluginId = pluginId,
            message = "Plugin '$pluginId' is not supported"
        )
        return try {
            val item = action(runtime)
            recordActivity(
                type = activityType,
                plugin = runtime.definition,
                description = buildDescription(activityType, runtime.definition.manifest.name),
                status = ActivityStatus.SUCCESS,
                metadata = mapOf(
                    "pluginId" to pluginId,
                    "installed" to item.installed.toString(),
                    "attached" to item.attached.toString(),
                    "enabled" to item.enabled.toString(),
                    "state" to item.lifecycleState.name
                )
            )
            Result.success(item)
        } catch (e: Exception) {
            recordFailure(activityType, pluginId, e.message ?: "Plugin action failed", e)
        }
    }

    private fun recordFailure(
        activityType: ActivityType,
        pluginId: String,
        message: String,
        throwable: Throwable? = null
    ): Result<PluginCatalogItem> {
        val runtime = plugins[pluginId]
        val definition = runtime?.definition ?: UnsupportedPluginDefinition(pluginId)
        if (runtime != null) {
            runtime.lastActionMessage = message
        }
        runBlockingRecordActivity(
            type = activityType,
            plugin = definition,
            description = buildDescription(activityType, definition.manifest.name),
            status = ActivityStatus.FAILED,
            metadata = mapOf(
                "pluginId" to pluginId,
                "error" to message,
                "detail" to (throwable?.message ?: message)
            )
        )
        return Result.failure(IllegalStateException(message, throwable))
    }

    private fun buildDescription(activityType: ActivityType, pluginName: String): String = when (activityType) {
        ActivityType.PLUGIN_INSTALLED -> "Installed plugin: $pluginName"
        ActivityType.PLUGIN_ATTACHED -> "Attached plugin: $pluginName"
        ActivityType.PLUGIN_DETACHED -> "Detached plugin: $pluginName"
        ActivityType.PLUGIN_ENABLED -> "Enabled plugin: $pluginName"
        ActivityType.PLUGIN_DISABLED -> "Disabled plugin: $pluginName"
        ActivityType.PLUGIN_REMOVED -> "Removed plugin: $pluginName"
        ActivityType.PLUGIN_VALIDATED -> "Validated plugin: $pluginName"
        ActivityType.PLUGIN_HEALTH_CHECKED -> "Checked plugin health: $pluginName"
        else -> pluginName
    }

    private suspend fun recordActivity(
        type: ActivityType,
        plugin: PluginDefinition,
        description: String,
        status: ActivityStatus,
        metadata: Map<String, String> = emptyMap()
    ) {
        activityRepository.create(
            Activity(
                id = UUID.randomUUID(),
                type = type,
                entityType = PLUGIN_ENTITY_TYPE,
                entityId = entityId(plugin.manifest.id),
                description = description,
                metadata = metadata,
                createdAt = Instant.now(),
                status = status
            )
        )
    }

    private fun runBlockingRecordActivity(
        type: ActivityType,
        plugin: PluginDefinition,
        description: String,
        status: ActivityStatus,
        metadata: Map<String, String> = emptyMap()
    ) {
        kotlinx.coroutines.runBlocking {
            recordActivity(type, plugin, description, status, metadata)
        }
    }

    private fun ensureInstalled(runtime: PluginRuntimeState, action: String) {
        require(runtime.installed) { "Cannot $action plugin that is not installed" }
    }

    private fun ensureAttached(runtime: PluginRuntimeState, action: String) {
        require(runtime.attached) { "Cannot $action plugin that is not attached" }
    }

    private fun ensureSuccessfulLifecycle(report: PluginLifecycleReport, action: String) {
        if (report.state == PluginLifecycleState.FAILED || report.state == PluginLifecycleState.BLOCKED) {
            val diagnostic = report.diagnostics.firstOrNull()?.message ?: "lifecycle action failed"
            throw IllegalStateException("Cannot $action plugin: $diagnostic")
        }
    }

    private fun PluginRuntimeState.toCatalogItem(
        lifecycleState: PluginLifecycleState = host.lifecycleState(definition.manifest.id) ?: PluginLifecycleState.DISCOVERED,
        lastActionMessage: String? = this.lastActionMessage
    ): PluginCatalogItem = PluginCatalogItem(
        id = definition.manifest.id,
        name = definition.manifest.name,
        description = definition.manifest.description
            ?: definition.manifest.extensionPoints.firstOrNull()?.description
            ?: "Optional add-on capability",
        version = definition.manifest.version,
        configurationScope = definition.manifest.configurationScope,
        configurationSchema = definition.manifest.configurationSchema,
        optional = definition.manifest.optional,
        installed = installed,
        attached = attached,
        enabled = enabled,
        lifecycleState = lifecycleState,
        lastActionMessage = lastActionMessage
    )

    private fun PluginRuntimeState.toCatalogItem(report: PluginLifecycleReport): PluginCatalogItem =
        toCatalogItem(report.state, report.diagnostics.firstOrNull()?.message ?: lastActionMessage)

    private fun entityId(pluginId: String): UUID =
        UUID.nameUUIDFromBytes(pluginId.toByteArray())

    private class UnsupportedPluginDefinition(pluginId: String) : PluginDefinition {
        override val manifest: PluginManifest = PluginManifest(
            id = pluginId,
            name = pluginId,
            version = "unknown",
            requiredHostContractVersion = PluginContractVersion(1, 0)
        )

        override suspend fun initialize(context: PluginHostContext): PluginLifecycleReport =
            error("Unsupported plugin")

        override suspend fun enable(context: PluginHostContext): PluginLifecycleReport =
            error("Unsupported plugin")

        override suspend fun disable(context: PluginHostContext): PluginLifecycleReport =
            error("Unsupported plugin")

        override suspend fun remove(context: PluginHostContext): PluginLifecycleReport =
            error("Unsupported plugin")

        override suspend fun health(context: PluginHostContext): PluginHealthReport =
            PluginHealthReport(
                pluginId = manifest.id,
                state = HealthState.FAILED,
                message = "Unsupported plugin"
            )
    }

    private class NoOpPluginPrerequisiteProbe : PluginPrerequisiteProbe {
        override suspend fun evaluate(
            prerequisite: PluginPrerequisite,
            pluginConfiguration: Map<String, String>
        ): PluginPrerequisiteResult = PluginPrerequisiteResult(
            prerequisite = prerequisite,
            satisfied = true,
            message = "${prerequisite.name} assumed available for in-memory defaults"
        )
    }
}
