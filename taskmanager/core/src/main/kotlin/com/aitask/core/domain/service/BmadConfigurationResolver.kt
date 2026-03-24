package com.aitask.core.domain.service

import com.aitask.core.domain.model.BmadConfiguration
import com.aitask.core.domain.model.Methodology
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Task

class BmadConfigurationResolver {
    fun resolve(project: Project, task: Task): BmadConfiguration {
        val methodology = task.effectiveMethodology(project.methodology)
        val toolIds = task.effectiveBmadToolIds(project.bmadToolIds)
        val injectionEnabled = task.bmadInjectionEnabledOverride ?: (methodology == Methodology.BMAD)

        return BmadConfiguration(
            methodology = methodology,
            toolIds = toolIds,
            injectionEnabled = injectionEnabled,
            methodologyOverridden = task.methodologyOverride != null,
            toolsOverridden = task.bmadToolOverrideIds != null,
            injectionOverridden = task.bmadInjectionEnabledOverride != null
        )
    }
}
