package com.aitask.core.domain.plugin

data class PluginVisibleAndInternalFields(
    val visible: Map<String, String>,
    val internal: Map<String, String>
)

fun splitPluginConfigurationFields(
    plugin: PluginCatalogItem,
    snapshotFields: Map<String, String>
): PluginVisibleAndInternalFields {
    val schemaIds: Set<String> = plugin.configurationSchema.fields.map { it.id }.toSet()
    val visible: Map<String, String> = snapshotFields.filterKeys { key: String -> key in schemaIds }
    val internal: Map<String, String> = snapshotFields.filterKeys { key: String -> key !in schemaIds }
    return PluginVisibleAndInternalFields(visible = visible, internal = internal)
}

fun mergePluginConfigurationFields(
    visible: Map<String, String>,
    internal: Map<String, String>
): Map<String, String> = visible + internal
