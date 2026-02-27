package com.peakai.fitness.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakai.fitness.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogUiState(
    val waterMl: Int = 0,
    val caffeineMg: Int = 0,
    val supplements: List<String> = emptyList(),
    val medications: List<String> = emptyList()
)

@HiltViewModel
class LogViewModel @Inject constructor(
    private val repository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    init {
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            repository.observeTodayLogs().collect { summary ->
                _uiState.update {
                    it.copy(
                        waterMl = summary.waterMl,
                        caffeineMg = summary.caffeineMg,
                        supplements = summary.supplements,
                        medications = summary.medications
                    )
                }
            }
        }
    }

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            repository.logWater(amountMl)
        }
    }

    fun logCaffeine(amountMg: Int, source: String = "") {
        viewModelScope.launch {
            repository.logCaffeine(amountMg, source)
        }
    }

    fun logSupplement(name: String) {
        viewModelScope.launch {
            repository.logSupplement(name)
        }
    }

    fun logMedication(name: String) {
        viewModelScope.launch {
            repository.logMedication(name)
        }
    }
}
