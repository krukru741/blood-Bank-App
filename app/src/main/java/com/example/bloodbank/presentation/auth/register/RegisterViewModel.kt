package com.example.bloodbank.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.User
import com.example.bloodbank.domain.model.UserRole
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.domain.repository.UserRepository
import com.example.bloodbank.presentation.common.AuthUiState
import com.example.bloodbank.presentation.common.validation.FormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState<Unit>>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState<Unit>> = _uiState.asStateFlow()

    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ) {
        if (!validateStep1(fullName, email, phone, password, confirmPassword)) return

        viewModelScope.launch {
            authRepository.register(
                email       = email,
                password    = password,
                displayName = fullName,
                phoneNumber = phone
            ).collect { authResource ->
                when (authResource) {
                    is Resource.Loading -> _uiState.value = AuthUiState.Loading
                    is Resource.Success -> {
                        val fullUser = authResource.data.copy(
                            role = UserRole.USER
                        )
                        saveProfile(fullUser)
                    }
                    is Resource.Error -> _uiState.value = AuthUiState.Error(
                        authResource.error.message.toFriendlyMessage()
                    )
                }
            }
        }
    }

    private suspend fun saveProfile(user: User) {
        userRepository.saveUserProfile(user).collect { saveResource ->
            when (saveResource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    sendVerificationEmail()
                    _uiState.value = AuthUiState.Success(Unit)
                }
                is Resource.Error -> _uiState.value = AuthUiState.Error(
                    "Account created but profile save failed."
                )
            }
        }
    }

    private suspend fun sendVerificationEmail() {
        authRepository.sendEmailVerification().collect { }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun validateStep1(
        fullName: String, email: String, phone: String, password: String, confirmPassword: String
    ): Boolean {
        val nameError     = FormValidator.validateFullName(fullName)
        val emailError    = FormValidator.validateEmail(email)
        val phoneError    = FormValidator.validatePhoneNumber(phone)
        val passwordError = FormValidator.validatePassword(password)
        val confirmError  = FormValidator.validateConfirmPassword(password, confirmPassword)

        val firstError = listOf(nameError, emailError, phoneError, passwordError, confirmError)
            .firstOrNull { it != null }

        return if (firstError != null) {
            _uiState.value = AuthUiState.Error(firstError)
            false
        } else true
    }
}

private fun String.toFriendlyMessage(): String = when {
    contains("email address is already in use", ignoreCase = true) -> "An account with this email already exists."
    contains("email address is badly formatted", ignoreCase = true) -> "Please enter a valid email address."
    contains("password should be at least", ignoreCase = true) -> "Password must be at least 8 characters."
    contains("network error", ignoreCase = true) -> "Network error. Check your connection."
    else -> "Registration failed. Please try again."
}
