package com.aitask.core.domain.service

import com.aitask.core.domain.model.LlmConnectionTestResult

interface LlmConnectionValidator {
    suspend fun validate(endpointUrl: String, apiKey: String? = null): Result<LlmConnectionTestResult>
}
