package com.aitask.desktop.ui.viewmodel

import com.aitask.core.domain.model.HealthStatus
import com.aitask.core.domain.service.HealthCheckService
import com.aitask.desktop.di.DependencyContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IntegrationsViewModel(
    private val healthCheckService: HealthCheckService = DependencyContainer.healthCheckService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _healthStatus = MutableStateFlow<HealthStatus?>(null)
    val healthStatus: StateFlow<HealthStatus?> = _healthStatus.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadHealth() {
        scope.launch {
            runHealthChecks()
        }
    }

    fun refreshHealth() {
        scope.launch {
            runHealthChecks()
        }
    }

    private suspend fun runHealthChecks() {
        _isLoading.value = true
        _error.value = null
        try {
            val status = healthCheckService.runHealthChecks()
            _healthStatus.value = status
        } catch (e: Exception) {
            _error.value = e.message ?: "Health check failed"
        } finally {
            _isLoading.value = false
        }
    }
}
