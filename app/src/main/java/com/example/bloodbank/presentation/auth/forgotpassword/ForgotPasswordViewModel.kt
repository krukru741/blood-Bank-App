package com.example.bloodbank.presentation.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.presentation.common.AuthUiState
import com.example.bloodbank.presentation.common.validation.FormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ForgotPasswordViewModel
 *
 * Calls [AuthRepository.sendPasswordResetEmail] and exposes [uiState]
 * as StateFlow<AuthUiState<Unit>>.
 *
 * Success means the email was sent (Firebase doesn't confirm if the
 * email actually exists — by design, to prevent user enumeration).
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState<Unit>>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState<Unit>> = _uiState.asStateFlow()

    fun sendResetEmail(email: String) {
        val emailError = FormValidator.validateEmail(email)
        if (emailError != null) {
            _uiState.value = AuthUiState.Error(emailError)
            return
        }

        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email).collect { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> AuthUiState.Loading
                    is Resource.Success -> AuthUiState.Success(Unit)
                    is Resource.Error   -> AuthUiState.Error(
                        when {
                            resource.error.message.contains("network error", ignoreCase = true) ->
                                "Network error. Please check your internet connection."
                            else -> "Failed to send reset email. Please try again."
                        }
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
