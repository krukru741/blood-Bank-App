package com.example.bloodbank.presentation.auth.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.User
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.presentation.common.AuthUiState
import com.example.bloodbank.presentation.common.validation.FormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState<User>>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState<User>> = _uiState.asStateFlow()

    private val _savedEmail = MutableStateFlow<String?>(null)
    val savedEmail: StateFlow<String?> = _savedEmail.asStateFlow()

    private val emailKey = stringPreferencesKey("remembered_email")

    init {
        // Load saved email on init
        viewModelScope.launch {
            val email = dataStore.data.map { it[emailKey] }.firstOrNull()
            _savedEmail.value = email
        }
    }

    fun login(email: String, password: String, rememberMe: Boolean) {
        val emailError    = FormValidator.validateEmail(email)
        val passwordError = FormValidator.validatePassword(password)

        if (!FormValidator.allValid(emailError, passwordError)) {
            _uiState.value = AuthUiState.Error(
                emailError ?: passwordError ?: "Invalid input"
            )
            return
        }

        viewModelScope.launch {
            authRepository.login(email, password).collect { resource ->
                if (resource is Resource.Success && rememberMe) {
                    saveEmail(email)
                } else if (resource is Resource.Success && !rememberMe) {
                    clearSavedEmail()
                }

                _uiState.value = when (resource) {
                    is Resource.Loading -> AuthUiState.Loading
                    is Resource.Success -> AuthUiState.Success(resource.data)
                    is Resource.Error   -> AuthUiState.Error(resource.error.message.toFriendlyMessage())
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken).collect { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> AuthUiState.Loading
                    is Resource.Success -> AuthUiState.Success(resource.data)
                    is Resource.Error   -> AuthUiState.Error(resource.error.message.toFriendlyMessage())
                }
            }
        }
    }

    fun signInWithFacebook(accessToken: String) {
        viewModelScope.launch {
            authRepository.signInWithFacebook(accessToken).collect { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> AuthUiState.Loading
                    is Resource.Success -> AuthUiState.Success(resource.data)
                    is Resource.Error   -> AuthUiState.Error(resource.error.message.toFriendlyMessage())
                }
            }
        }
    }

    private suspend fun saveEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[emailKey] = email
        }
    }

    private suspend fun clearSavedEmail() {
        dataStore.edit { preferences ->
            preferences.remove(emailKey)
        }
    }

    /** Resets state to Idle (called after navigation or error dismissed). */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

// ── Map Firebase error codes → friendly user messages ─────────────────────────
private fun String.toFriendlyMessage(): String = when {
    contains("no user record", ignoreCase = true)       -> "No account found with this email."
    contains("password is invalid", ignoreCase = true)  -> "Incorrect password. Please try again."
    contains("network error", ignoreCase = true)        -> "Network error. Check your connection."
    contains("too many requests", ignoreCase = true)    -> "Too many attempts. Please try again later."
    contains("user disabled", ignoreCase = true)        -> "This account has been disabled."
    contains("verify your email", ignoreCase = true)    -> "Please verify your email address before logging in."
    else -> "Login failed. Please try again."
}
