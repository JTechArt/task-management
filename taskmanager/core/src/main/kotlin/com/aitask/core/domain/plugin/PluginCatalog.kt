package com.aitask.core.domain.plugin

data class PluginCatalogItem(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val configurationScope: PluginConfigurationScope,
    val configurationSchema: PluginConfigurationSchema,
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
                configurationScope = PluginConfigurationScope.APP,
                configurationSchema = PluginConfigurationSchema(
                    fields = listOf(
                        PluginConfigurationField(
                            id = "review_summary_prefix",
                            label = "Summary Prefix",
                            type = PluginConfigurationFieldType.TEXT,
                            description = "Prefix used in review summaries.",
                            placeholder = "Review:"
                        ),
                        PluginConfigurationField(
                            id = "review_token",
                            label = "Review Token",
                            type = PluginConfigurationFieldType.SECRET,
                            required = true,
                            description = "Token used to authenticate external review services."
                        ),
                        PluginConfigurationField(
                            id = "schedule",
                            label = "Refresh Schedule",
                            type = PluginConfigurationFieldType.SCHEDULE,
                            description = "Optional cron-like schedule for background refreshes.",
                            placeholder = "0 */6 * * *"
                        )
                    ),
                    prerequisites = listOf(
                        PluginPrerequisite(
                            type = PluginPrerequisiteType.LOCAL_BINARY,
                            name = "git",
                            value = "git",
                            remediation = "Install Git so the review assistant can inspect repository changes."
                        ),
                        PluginPrerequisite(
                            type = PluginPrerequisiteType.CREDENTIAL,
                            name = "REVIEW_API_TOKEN",
                            value = "REVIEW_API_TOKEN",
                            remediation = "Set REVIEW_API_TOKEN before enabling the review assistant."
                        )
                    ),
                    validationRules = listOf(
                        PluginValidationRule(
                            id = "review-token-required",
                            description = "Requires an API token and a summary prefix before enablement.",
                            requiredFields = listOf("review_summary_prefix"),
                            requiredPrerequisites = emptyList()
                        )
                    )
                ),
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
                configurationScope = PluginConfigurationScope.PROJECT,
                configurationSchema = PluginConfigurationSchema(
                    fields = listOf(
                        PluginConfigurationField(
                            id = "cleanup_mode",
                            label = "Cleanup Mode",
                            type = PluginConfigurationFieldType.SELECT,
                            required = true,
                            options = listOf("safe", "balanced", "aggressive"),
                            description = "Select how aggressively stale files are removed."
                        ),
                        PluginConfigurationField(
                            id = "retain_days",
                            label = "Retain Days",
                            type = PluginConfigurationFieldType.TEXT,
                            required = true,
                            description = "How many days of workspace history to retain.",
                            placeholder = "14"
                        ),
                        PluginConfigurationField(
                            id = "cleanup_schedule",
                            label = "Cleanup Schedule",
                            type = PluginConfigurationFieldType.SCHEDULE,
                            description = "Optional cron-like cleanup schedule.",
                            placeholder = "0 2 * * *"
                        )
                    ),
                    prerequisites = listOf(
                        PluginPrerequisite(
                            type = PluginPrerequisiteType.LOCAL_BINARY,
                            name = "node",
                            value = "node",
                            remediation = "Install Node.js so the workspace optimizer can run maintenance tasks."
                        ),
                        PluginPrerequisite(
                            type = PluginPrerequisiteType.COMPANION_APP,
                            name = "IDE",
                            value = "Cursor",
                            remediation = "Install or configure an IDE so the workspace optimizer can sync workspace state."
                        )
                    )
                ),
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
                configurationScope = PluginConfigurationScope.APP,
                configurationSchema = PluginConfigurationSchema(
                    fields = listOf(
                        PluginConfigurationField(
                            id = "release_prefix",
                            label = "Release Prefix",
                            type = PluginConfigurationFieldType.TEXT,
                            description = "Prefix used when generating release notes.",
                            placeholder = "Release"
                        ),
                        PluginConfigurationField(
                            id = "release_token",
                            label = "Release Token",
                            type = PluginConfigurationFieldType.SECRET,
                            required = true,
                            description = "Token for the release notes companion service."
                        )
                    ),
                    prerequisites = listOf(
                        PluginPrerequisite(
                            type = PluginPrerequisiteType.ENDPOINT,
                            name = "Release service",
                            value = "https://example.invalid/releases/health",
                            remediation = "Configure a reachable release notes service endpoint."
                        )
                    )
                ),
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
        ),
        PluginSeed(
            definition = samplePlugin(
                id = "plugin.slack-channel-analyzer",
                name = "Slack Channel Analyzer",
                description = "Summarizes Slack channel activity on a schedule you control.",
                version = "1.0.0",
                configurationScope = PluginConfigurationScope.APP,
                configurationSchema = PluginConfigurationSchema(
                    fields = listOf(
                        PluginConfigurationField(
                            id = "slack_bot_token",
                            label = "Slack Bot Token",
                            type = PluginConfigurationFieldType.SECRET,
                            required = true,
                            description = "Bot token (xoxb-) with channels:read and conversations access for validation.",
                            placeholder = "xoxb-..."
                        ),
                        PluginConfigurationField(
                            id = "analysis_channels",
                            label = "Channels to analyze",
                            type = PluginConfigurationFieldType.TEXT,
                            required = true,
                            description = "Comma-separated Slack channel IDs (for example C0123456789).",
                            placeholder = "C0123456789, C0987654321"
                        ),
                        PluginConfigurationField(
                            id = "schedule_mode",
                            label = "Run mode",
                            type = PluginConfigurationFieldType.SELECT,
                            required = true,
                            options = listOf("manual_only", "daily", "manual_and_daily"),
                            description = "Manual runs only, daily scheduled runs, or both."
                        ),
                        PluginConfigurationField(
                            id = "daily_run_time",
                            label = "Daily run time (local)",
                            type = PluginConfigurationFieldType.TEXT,
                            required = false,
                            description = "Time for daily runs in 24-hour form (for example 10:00 for 10:00 AM).",
                            placeholder = "10:00"
                        )
                    ),
                    prerequisites = listOf(
                        PluginPrerequisite(
                            type = PluginPrerequisiteType.SLACK_API,
                            name = "Slack connectivity",
                            value = "slack_bot_token",
                            remediation = "Provide a valid Slack bot token with required scopes."
                        ),
                        PluginPrerequisite(
                            type = PluginPrerequisiteType.SLACK_CHANNELS,
                            name = "Channel accessibility",
                            value = "analysis_channels",
                            remediation = "Use valid channel IDs and invite the bot to private channels."
                        )
                    ),
                    validationRules = listOf(
                        PluginValidationRule(
                            id = "daily-run-time-required",
                            description = "Daily runs require a local run time",
                            requiredFields = listOf("daily_run_time"),
                            whenFieldId = "schedule_mode",
                            whenFieldMatches = listOf("daily", "manual_and_daily")
                        )
                    )
                ),
                installed = true,
                extensionPoints = listOf(
                    PluginExtensionPointDeclaration(
                        type = PluginExtensionPointType.UI_SURFACE,
                        id = "slack-analyzer-source-schedule",
                        label = "Slack Analyzer — Source & Schedule"
                    )
                )
            ),
            installed = true
        )
    )

    private fun samplePlugin(
        id: String,
        name: String,
        description: String,
        version: String,
        configurationScope: PluginConfigurationScope,
        configurationSchema: PluginConfigurationSchema,
        installed: Boolean,
        extensionPoints: List<PluginExtensionPointDeclaration>
    ): PluginDefinition = object : PluginDefinition {
        override val manifest: PluginManifest = PluginManifest(
            id = id,
            name = name,
            version = version,
            description = description,
            requiredHostContractVersion = PluginContractVersion(1, 0),
            configurationScope = configurationScope,
            configurationSchema = configurationSchema,
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
