package com.aitask.core.domain.model

data class SlackChannelMessage(
    val ts: String,
    val text: String,
    val threadTs: String? = null,
    val userId: String? = null
)
