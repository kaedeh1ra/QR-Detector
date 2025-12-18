package ru.kaed.fishing.link.detector.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.kaed.fishing.link.core.detector.domain.model.UrlAnalysisResult
import ru.kaed.fishing.link.core.detector.domain.usecase.ScanUrlUseCase

sealed interface ScannerUiState {
    data object Idle : ScannerUiState
    data object Loading : ScannerUiState
    data class Success(val result: UrlAnalysisResult) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

class ScannerViewModel(
    private val scanUrlUseCase: ScanUrlUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun onQrScanned(url: String) {
        if (_uiState.value is ScannerUiState.Loading) return

        _uiState.value = ScannerUiState.Loading

        viewModelScope.launch {
            try {
                val result = scanUrlUseCase(url)
                _uiState.value = ScannerUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _uiState.value = ScannerUiState.Idle
    }
}