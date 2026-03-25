package com.aitask.core.domain.repository

import com.aitask.core.domain.model.GeppaConfiguration
import com.aitask.core.domain.model.GeppaConfigurationRequest

interface GeppaConfigurationRepository {
    suspend fun find(): GeppaConfiguration?
    suspend fun save(request: GeppaConfigurationRequest): GeppaConfiguration
}
