package com.aitask.desktop.ui.viewmodel

import com.aitask.core.domain.model.ComponentHealth
import com.aitask.core.domain.model.HealthState
import com.aitask.core.domain.model.HealthStatus
import com.aitask.core.domain.model.OAuthProvider
import com.aitask.core.domain.service.HealthCheckService
import com.aitask.core.domain.usecase.GetOAuthStatusUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationsViewModelTest {

    private val healthCheckService = mockk<HealthCheckService>()
    private val getOAuthStatusUseCase = mockk<GetOAuthStatusUseCase>()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: IntegrationsViewModel

    @BeforeEach
    fun setUp() {
        coEvery { getOAuthStatusUseCase.getStatusForProviders(listOf(OAuthProvider.SLACK)) } returns emptyList()
        viewModel = IntegrationsViewModel(
            healthCheckService = healthCheckService,
            getOAuthStatusUseCase = getOAuthStatusUseCase,
            scope = CoroutineScope(testDispatcher)
        )
    }

    @Test
    fun `loadHealth fetches and stores health status`() = runTest(testDispatcher) {
        val expectedStatus = createHealthStatus()
        coEvery { healthCheckService.runHealthChecks() } returns expectedStatus
        viewModel.loadHealth()
        advanceUntilIdle()
        val status = viewModel.healthStatus.first()
        assertNotNull(status)
        assertEquals(expectedStatus.database.state, status?.database?.state)
        assertEquals(expectedStatus.repositories.state, status?.repositories?.state)
        assertEquals(expectedStatus.ideDiscovery.state, status?.ideDiscovery?.state)
        assertFalse(viewModel.isLoading.first())
        assertNull(viewModel.error.first())
    }

    @Test
    fun `refreshHealth triggers health check`() = runTest(testDispatcher) {
        val expectedStatus = createHealthStatus()
        coEvery { healthCheckService.runHealthChecks() } returns expectedStatus
        viewModel.refreshHealth()
        advanceUntilIdle()
        coVerify(exactly = 1) { healthCheckService.runHealthChecks() }
        assertNotNull(viewModel.healthStatus.first())
        assertFalse(viewModel.isLoading.first())
    }

    @Test
    fun `loadHealth sets error when health check fails`() = runTest(testDispatcher) {
        val errorMessage = "Connection refused"
        coEvery { healthCheckService.runHealthChecks() } throws RuntimeException(errorMessage)
        viewModel.loadHealth()
        advanceUntilIdle()
        assertEquals(errorMessage, viewModel.error.first())
        assertNull(viewModel.healthStatus.first())
        assertFalse(viewModel.isLoading.first())
    }

    private fun createHealthStatus(): HealthStatus {
        val dbHealth = ComponentHealth(
            id = "database",
            label = "Database",
            state = HealthState.HEALTHY,
            message = "Connected",
            detail = "5 ms"
        )
        val repoHealth = ComponentHealth(
            id = "repositories",
            label = "Repositories",
            state = HealthState.HEALTHY,
            message = "0 of 0 repositories accessible",
            detail = null
        )
        val ideHealth = ComponentHealth(
            id = "ide-discovery",
            label = "IDE discovery",
            state = HealthState.HEALTHY,
            message = "1 IDE detected",
            detail = "CURSOR"
        )
        return HealthStatus(
            database = dbHealth,
            repositories = repoHealth,
            repositoryDetails = emptyList(),
            ideDiscovery = ideHealth,
            lastRefreshedAt = Instant.now()
        )
    }
}
