package com.example.bloodbank.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.User
import com.example.bloodbank.domain.model.UserRole
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
    data object SignedOut : ProfileUiState()
}

/**
 * ProfileViewModel
 *
 * - Fetches the current user's full profile from Firestore (real-time stream)
 * - Handles sign-out
 * - Computes donor stats (eligibility, donations count)
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = authRepository.currentUser?.uid ?: run {
            _uiState.value = ProfileUiState.Error("Not logged in")
            return
        }

        viewModelScope.launch {
            userRepository.observeCurrentUser(uid).collect { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> ProfileUiState.Loading
                    is Resource.Success -> ProfileUiState.Success(resource.data)
                    is Resource.Error   -> ProfileUiState.Error(resource.error.message)
                }
            }
        }
    }

    fun toggleAvailability(isAvailable: Boolean) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            val updatedUser = currentState.user.copy(isAvailableToDonate = isAvailable)
            viewModelScope.launch {
                userRepository.saveUserProfile(updatedUser).collect { resource ->
                    if (resource is Resource.Error) {
                        _uiState.value = ProfileUiState.Error("Failed to update availability: ${resource.error.message}")
                    }
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = ProfileUiState.SignedOut
        }
    }
}
