package com.aitask.core.domain.service

import com.aitask.core.domain.model.GeppaConnectionTestResult

interface GeppaConnectionValidator {
    suspend fun validate(endpointUrl: String): Result<GeppaConnectionTestResult>
}
