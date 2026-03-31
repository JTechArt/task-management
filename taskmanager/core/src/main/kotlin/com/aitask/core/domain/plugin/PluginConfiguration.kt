package com.aitask.core.domain.plugin

import kotlinx.serialization.Serializable

@Serializable
enum class PluginConfigurationScope {
    APP,
    PROJECT
}

@Serializable
enum class PluginConfigurationFieldType {
    TEXT,
    SECRET,
    BOOLEAN,
    SELECT,
    SCHEDULE
}

@Serializable
data class PluginConfigurationField(
    val id: String,
    val label: String,
    val type: PluginConfigurationFieldType,
    val required: Boolean = false,
    val description: String? = null,
    val placeholder: String? = null,
    val options: List<String> = emptyList()
) {
    init {
        require(id.isNotBlank()) { "field id must not be blank" }
        require(label.isNotBlank()) { "field label must not be blank" }
    }
}

@Serializable
enum class PluginPrerequisiteType {
    LOCAL_BINARY,
    ENDPOINT,
    CREDENTIAL,
    COMPANION_APP,
    SLACK_API,
    SLACK_CHANNELS
}

@Serializable
data class PluginPrerequisite(
    val type: PluginPrerequisiteType,
    val name: String,
    val value: String,
    val remediation: String
) {
    init {
        require(name.isNotBlank()) { "prerequisite name must not be blank" }
        require(value.isNotBlank()) { "prerequisite value must not be blank" }
        require(remediation.isNotBlank()) { "prerequisite remediation must not be blank" }
    }
}

@Serializable
data class PluginValidationRule(
    val id: String,
    val description: String,
    val requiredFields: List<String> = emptyList(),
    val requiredPrerequisites: List<PluginPrerequisite> = emptyList(),
    val whenFieldId: String? = null,
    val whenFieldMatches: List<String> = emptyList()
)

@Serializable
data class PluginConfigurationSchema(
    val fields: List<PluginConfigurationField> = emptyList(),
    val prerequisites: List<PluginPrerequisite> = emptyList(),
    val validationRules: List<PluginValidationRule> = emptyList()
)

@Serializable
enum class PluginConfigurationValidationStatus {
    UNKNOWN,
    VALID,
    INVALID
}

@Serializable
data class PluginConfigurationSnapshot(
    val pluginId: String,
    val scope: PluginConfigurationScope,
    val scopeKey: String? = null,
    val fields: Map<String, String> = emptyMap(),
    val secrets: Map<String, String> = emptyMap(),
    val schedule: String? = null,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val lastValidatedAtEpochMillis: Long? = null,
    val validationStatus: PluginConfigurationValidationStatus = PluginConfigurationValidationStatus.UNKNOWN,
    val validationMessage: String? = null
) {
    init {
        require(pluginId.isNotBlank()) { "plugin id must not be blank" }
    }
}

data class PluginPrerequisiteResult(
    val prerequisite: PluginPrerequisite,
    val satisfied: Boolean,
    val message: String,
    val remediation: String? = null
)

data class PluginConfigurationValidationResult(
    val valid: Boolean,
    val message: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val prerequisiteResults: List<PluginPrerequisiteResult> = emptyList(),
    val ruleMessages: List<String> = emptyList(),
    val lastKnownGoodSnapshot: PluginConfigurationSnapshot? = null
)

class PluginConfigurationValidationException(
    val validationResult: PluginConfigurationValidationResult
) : Exception(validationResult.message ?: "Plugin configuration validation failed")
