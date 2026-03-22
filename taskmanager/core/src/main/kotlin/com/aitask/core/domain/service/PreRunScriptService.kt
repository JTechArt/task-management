package com.aitask.core.domain.service

import com.aitask.core.domain.model.PreRunExecutionResult
import com.aitask.core.domain.model.PreRunScript

interface PreRunScriptService {
    suspend fun executeScripts(
        scripts: List<PreRunScript>,
        workingDirectory: String
    ): Result<List<PreRunExecutionResult>>
}
