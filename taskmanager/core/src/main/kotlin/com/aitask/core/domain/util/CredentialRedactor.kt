package com.aitask.core.domain.util

object CredentialRedactor {

    @JvmStatic
    fun redactUrl(url: String): String {
        return EMBEDDED_CREDENTIALS_REGEX.replace(url) { matchResult ->
            matchResult.groupValues[1] + "***" + matchResult.groupValues[3]
        }
    }

    @JvmStatic
    fun redactMessage(message: String): String {
        return EMBEDDED_CREDENTIALS_REGEX.replace(message) { matchResult ->
            matchResult.groupValues[1] + "***" + matchResult.groupValues[3]
        }
    }

    private val EMBEDDED_CREDENTIALS_REGEX = Regex("(https?://)([^@]+)(@)")
}
