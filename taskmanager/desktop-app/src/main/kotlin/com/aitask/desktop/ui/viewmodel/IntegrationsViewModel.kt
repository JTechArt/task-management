package com.aitask.desktop.ui.viewmodel

import com.aitask.core.domain.model.HealthStatus
import com.aitask.core.domain.model.OAuthProvider
import com.aitask.core.domain.service.HealthCheckService
import com.aitask.core.domain.usecase.GetOAuthStatusUseCase
import com.aitask.desktop.di.DependencyContainer
import com.aitask.desktop.oauth.OAuthCallbackServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI

class IntegrationsViewModel(
    private val healthCheckService: HealthCheckService = DependencyContainer.healthCheckService,
    private val getOAuthStatusUseCase: GetOAuthStatusUseCase = DependencyContainer.getOAuthStatusUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _healthStatus = MutableStateFlow<HealthStatus?>(null)
    val healthStatus: StateFlow<HealthStatus?> = _healthStatus.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _oauthStatuses = MutableStateFlow<List<GetOAuthStatusUseCase.OAuthProviderStatus>>(emptyList())
    val oauthStatuses: StateFlow<List<GetOAuthStatusUseCase.OAuthProviderStatus>> = _oauthStatuses.asStateFlow()
    private val _oauthActionInProgress = MutableStateFlow<OAuthProvider?>(null)
    val oauthActionInProgress: StateFlow<OAuthProvider?> = _oauthActionInProgress.asStateFlow()

    fun loadHealth() {
        scope.launch {
            runHealthChecks()
            loadOAuthStatuses()
        }
    }

    fun refreshHealth() {
        scope.launch {
            runHealthChecks()
            loadOAuthStatuses()
        }
    }

    fun loadOAuthStatuses() {
        scope.launch {
            try {
                val statuses = getOAuthStatusUseCase.getStatusForProviders(listOf(OAuthProvider.SLACK))
                _oauthStatuses.value = statuses
            } catch (e: Exception) {
                _oauthStatuses.value = emptyList()
            }
        }
    }

    fun connectOAuth(provider: OAuthProvider) {
        scope.launch {
            _oauthActionInProgress.value = provider
            _error.value = null
            try {
                val result = DependencyContainer.oauthService.buildAuthorizationUrl(provider)
                result.fold(
                    onSuccess = { (url, state) -> _oauthActionInProgress.value = provider
                        val port = 38473
                        val callbackServer = OAuthCallbackServer(port, DependencyContainer.oauthService) { callbackResult ->
                            scope.launch {
                                withContext(Dispatchers.Main) {
                                    _oauthActionInProgress.value = null
                                    callbackResult.fold(
                                        onSuccess = { loadOAuthStatuses() },
                                        onFailure = { _error.value = it.message }
                                    )
                                }
                            }
                        }
                        callbackServer.start(provider, state)
                        Desktop.getDesktop().browse(URI(url))
                    },
                    onFailure = {
                        _oauthActionInProgress.value = null
                        _error.value = it.message
                    }
                )
            } catch (e: Exception) {
                _oauthActionInProgress.value = null
                _error.value = e.message
            }
        }
    }

    fun disconnectOAuth(provider: OAuthProvider) {
        scope.launch {
            try {
                DependencyContainer.oauthConnectionRepository.deleteByProvider(provider)
                loadOAuthStatuses()
            } catch (e: Exception) {
                _error.value = e.message
            }
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
