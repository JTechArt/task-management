package com.aitask.core.domain.model

data class BmadConfiguration(
    val methodology: Methodology,
    val toolIds: List<String>,
    val injectionEnabled: Boolean,
    val methodologyOverridden: Boolean,
    val toolsOverridden: Boolean,
    val injectionOverridden: Boolean
)
