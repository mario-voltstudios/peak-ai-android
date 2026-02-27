package com.peakai.fitness.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakai.fitness.coaching.GeminiNanoCoach
import com.peakai.fitness.data.repository.HealthRepository
import com.peakai.fitness.domain.model.CoachMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoachUiState(
    val messages: List<CoachMessage> = emptyList(),
    val isLoading: Boolean = false,
    val usingGeminiNano: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val geminiNano: GeminiNanoCoach
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    init {
        checkGeminiAvailability()
    }

    private fun checkGeminiAvailability() {
        viewModelScope.launch {
            val available = geminiNano.checkAvailability()
            _uiState.update { it.copy(usingGeminiNano = available) }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = CoachMessage(role = CoachMessage.Role.USER, content = text)
        _uiState.update {
            it.copy(messages = it.messages + userMsg, isLoading = true, error = null)
        }

        val history = _uiState.value.messages.map {
            Pair(if (it.role == CoachMessage.Role.USER) "User" else "Coach", it.content)
        }

        viewModelScope.launch {
            try {
                val response = repository.askCoach(text, history)
                val coachMsg = CoachMessage(
                    role = CoachMessage.Role.COACH,
                    content = response.content
                )
                _uiState.update {
                    it.copy(messages = it.messages + coachMsg, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Couldn't reach coach: ${e.message}"
                    )
                }
            }
        }
    }
}
