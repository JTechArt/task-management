package com.aitask.core.domain.service

/**
 * Redacts patterns that often appear in LLM or HTTP errors so activity logs stay safe to share (AC: no secrets in logs).
 */
object AutomationRunLogSanitizer {
    private const val MAX_ERROR_LENGTH: Int = 800
    private const val MAX_SUMMARY_LENGTH: Int = 400
    private const val MAX_STEP_TITLES_LENGTH: Int = 500
    private val bearerPattern: Regex = Regex("(?i)bearer\\s+\\S+")
    private val skOpenAiPattern: Regex = Regex("sk-[a-zA-Z0-9]{16,}")
    private val longHexTokenPattern: Regex = Regex("\\b[0-9a-fA-F]{32,}\\b")

    fun sanitizeErrorMessage(text: String): String {
        var t: String = text
        t = bearerPattern.replace(t, "Bearer [REDACTED]")
        t = skOpenAiPattern.replace(t, "[REDACTED]")
        t = longHexTokenPattern.replace(t, "[REDACTED]")
        if (t.length > MAX_ERROR_LENGTH) {
            t = t.take(MAX_ERROR_LENGTH) + "…"
        }
        return t
    }

    fun truncateForLog(text: String, maxLength: Int): String {
        val trimmed: String = text.trim()
        if (trimmed.length <= maxLength) {
            return trimmed
        }
        return trimmed.take(maxLength) + "…"
    }

    fun truncateApproachSummary(summary: String): String = truncateForLog(summary, MAX_SUMMARY_LENGTH)

    fun joinStepTitles(titles: List<String>): String =
        truncateForLog(titles.joinToString(" | "), MAX_STEP_TITLES_LENGTH)
}
