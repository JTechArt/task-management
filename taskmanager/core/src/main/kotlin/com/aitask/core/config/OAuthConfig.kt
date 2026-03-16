package com.aitask.core.config

/**
 * OAuth client configuration. Values from environment variables.
 * SLACK_CLIENT_ID, SLACK_CLIENT_SECRET: Slack app credentials
 * AITASK_OAUTH_REDIRECT_URI: default http://localhost:38473/oauth/callback
 */
data class OAuthConfig(
    val slackClientId: String? = System.getenv("SLACK_CLIENT_ID"),
    val slackClientSecret: String? = System.getenv("SLACK_CLIENT_SECRET"),
    val redirectUri: String = System.getenv("AITASK_OAUTH_REDIRECT_URI") ?: "http://localhost:38473/oauth/callback"
) {
    fun isSlackConfigured(): Boolean =
        !slackClientId.isNullOrBlank() && !slackClientSecret.isNullOrBlank()
}
