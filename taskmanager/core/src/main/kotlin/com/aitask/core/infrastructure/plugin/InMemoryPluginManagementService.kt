package com.aitask.core.infrastructure.plugin

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.HealthState
import com.aitask.core.domain.plugin.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.service.PluginManagementService
import java.time.Instant
import java.util.UUID

private const val PLUGIN_ENTITY_TYPE = "plugin"

class InMemoryPluginManagementService(
    private val activityRepository: ActivityRepository,
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
}
