package com.aitask.core.domain.plugin

data class PluginCatalogItem(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val optional: Boolean,
    val installed: Boolean,
    val attached: Boolean,
    val enabled: Boolean,
    val lifecycleState: PluginLifecycleState,
    val lastActionMessage: String? = null
) {
    val statusLabel: String
        get() = when {
            !installed -> "Available"
            enabled -> "Enabled"
            attached -> "Attached"
            lifecycleState == PluginLifecycleState.FAILED -> "Needs attention"
            lifecycleState == PluginLifecycleState.BLOCKED -> "Blocked"
            else -> "Installed"
        }

    val statusDescription: String
        get() = when {
            !installed -> "Not installed yet"
            enabled -> "Attached and active"
            attached -> "Installed and ready to enable"
            lifecycleState == PluginLifecycleState.FAILED -> "Last lifecycle action failed"
            lifecycleState == PluginLifecycleState.BLOCKED -> "Contract version mismatch"
            else -> "Installed but detached"
        }
}

data class PluginSeed(
    val definition: PluginDefinition,
    val installed: Boolean = false
)

object PluginCatalogFixtures {
    val coreFeatureHighlights: List<String> = listOf(
        "Dashboard",
        "Projects",
        "Tasks",
        "Activity",
        "Rules",
        "Integrations",
        "Settings"
    )

    fun defaultSeeds(): List<PluginSeed> = listOf(
        PluginSeed(
            definition = samplePlugin(
                id = "plugin.code-review-assistant",
                name = "Code Review Assistant",
                description = "Adds review-oriented guidance surfaces and helpers.",
                version = "1.0.0",
                installed = true,
                extensionPoints = listOf(
                    PluginExtensionPointDeclaration(
                        type = PluginExtensionPointType.UI_SURFACE,
                        id = "code-review-panel",
                        label = "Code Review Panel"
                    ),
                    PluginExtensionPointDeclaration(
                        type = PluginExtensionPointType.INTEGRATION_HOOK,
                        id = "pull-request-hook",
                        label = "Pull Request Hook"
                    )
                )
            ),
            installed = true
        ),
        PluginSeed(
            definition = samplePlugin(
                id = "plugin.workspace-optimizer",
                name = "Workspace Optimizer",
                description = "Keeps local workspaces tidy and ready for task execution.",
                version = "1.0.0",
                installed = true,
                extensionPoints = listOf(
                    PluginExtensionPointDeclaration(
                        type = PluginExtensionPointType.BACKGROUND_JOB,
                        id = "workspace-cleanup-job",
                        label = "Workspace Cleanup Job"
                    ),
                    PluginExtensionPointDeclaration(
                        type = PluginExtensionPointType.CONFIGURATION_SECTION,
                        id = "workspace-optimizer-settings",
                        label = "Workspace Optimizer Settings"
                    )
                )
            ),
            installed = true
        ),
        PluginSeed(
            definition = samplePlugin(
                id = "plugin.release-notes",
                name = "Release Notes Helper",
                description = "Generates release-ready notes from completed work items.",
                version = "1.0.0",
                installed = false,
                extensionPoints = listOf(
                    PluginExtensionPointDeclaration(
                        type = PluginExtensionPointType.UI_SURFACE,
                        id = "release-notes-view",
                        label = "Release Notes View"
                    )
                )
            ),
            installed = false
        )
    )

    private fun samplePlugin(
        id: String,
        name: String,
        description: String,
        version: String,
        installed: Boolean,
        extensionPoints: List<PluginExtensionPointDeclaration>
    ): PluginDefinition = object : PluginDefinition {
        override val manifest: PluginManifest = PluginManifest(
            id = id,
            name = name,
            version = version,
            description = description,
            requiredHostContractVersion = PluginContractVersion(1, 0),
            optional = true,
            extensionPoints = extensionPoints
        )

        override suspend fun initialize(context: PluginHostContext): PluginLifecycleReport =
            PluginLifecycleReport(
                pluginId = manifest.id,
                state = PluginLifecycleState.INITIALIZED,
                extensionPoints = manifest.extensionPoints
            )

        override suspend fun enable(context: PluginHostContext): PluginLifecycleReport =
            PluginLifecycleReport(
                pluginId = manifest.id,
                state = PluginLifecycleState.ENABLED,
                extensionPoints = context.extensionRegistry.findByPluginId(manifest.id)
            )

        override suspend fun disable(context: PluginHostContext): PluginLifecycleReport =
            PluginLifecycleReport(
                pluginId = manifest.id,
                state = PluginLifecycleState.DISABLED
            )

        override suspend fun remove(context: PluginHostContext): PluginLifecycleReport =
            PluginLifecycleReport(
                pluginId = manifest.id,
                state = PluginLifecycleState.REMOVED
            )

        override suspend fun health(context: PluginHostContext): PluginHealthReport =
            PluginHealthReport(
                pluginId = manifest.id,
                state = com.aitask.core.domain.model.HealthState.HEALTHY,
                message = description
            )
    }
}
