package com.aitask.core.domain.service

import com.aitask.core.domain.plugin.PluginPrerequisite
import com.aitask.core.domain.plugin.PluginPrerequisiteResult

interface PluginPrerequisiteProbe {
    suspend fun evaluate(prerequisite: PluginPrerequisite, pluginConfiguration: Map<String, String>): PluginPrerequisiteResult
}
