package com.peakai.fitness.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakai.fitness.data.repository.HealthRepository
import com.peakai.fitness.domain.model.BiometricSnapshot
import com.peakai.fitness.domain.model.CoachingMessage
import com.peakai.fitness.domain.model.DailyLogSummary
import com.peakai.fitness.domain.model.ReadinessScore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val hasPermissions: Boolean = false,
    val readiness: ReadinessScore? = null,
    val snapshot: BiometricSnapshot? = null,
    val morningBriefing: CoachingMessage? = null,
    val dailySummary: DailyLogSummary? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Track permission launcher contract
    val permissionContract = HealthConnectClient.Companion.ACTION_HEALTH_CONNECT_SETTINGS
    val requiredPermissions = repository.healthConnect.permissions

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val hasPerms = repository.healthConnect.hasAllPermissions()
                if (!hasPerms) {
                    _uiState.update { it.copy(isLoading = false, hasPermissions = false) }
                    return@launch
                }

                val snapshot = repository.getTodaySnapshot()
                val baseline = repository.getSevenDayBaseline()
                val readiness = repository.getReadinessScore(snapshot, baseline)
                val summary = repository.getDailySummary()
                val briefing = repository.getMorningBriefing()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasPermissions = true,
                        snapshot = snapshot,
                        readiness = readiness,
                        morningBriefing = briefing,
                        dailySummary = summary
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }

    fun requestPermissions() {
        // Trigger via HealthConnect permission launcher in UI
        // The Activity uses rememberLauncherForActivityResult with CreatePermissionRequest
        _uiState.update { it.copy(isLoading = false) }
    }

    fun onPermissionsGranted() {
        loadDashboard()
    }
}
