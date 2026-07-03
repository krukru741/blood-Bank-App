package com.example.bloodbank.presentation.request

import com.example.bloodbank.domain.model.BloodRequest

sealed class EditRequestUiState {
    data object Idle : EditRequestUiState()
    data object Loading : EditRequestUiState()
    data class Loaded(val request: BloodRequest) : EditRequestUiState()
    data object Updated : EditRequestUiState()
    data class Error(val message: String) : EditRequestUiState()
}
