package com.example.bloodbank.presentation.profile

import android.net.Uri
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EditProfileUiState {
    object Idle : EditProfileUiState()
    object Loading : EditProfileUiState()
    data class Success(val message: String) : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserById(uid).collect { resource ->
                if (resource is Resource.Success) {
                    _currentUser.value = resource.data
                }
            }
        }
    }

    fun updateProfile(
        displayName: String,
        phoneNumber: String,
        city: String,
        weightKgText: String,
        photoUri: Uri?
    ) {
        val user = _currentUser.value ?: return

        // Basic validation
        if (displayName.isBlank() || phoneNumber.isBlank() || city.isBlank()) {
            _uiState.value = EditProfileUiState.Error("Please fill in all required fields.")
            return
        }

        val weight = if (user.role == UserRole.DONOR) {
            val w = weightKgText.toFloatOrNull()
            if (w == null || w <= 0) {
                _uiState.value = EditProfileUiState.Error("Please enter a valid weight.")
                return
            }
            w
        } else {
            user.weightKg
        }

        _uiState.value = EditProfileUiState.Loading

        viewModelScope.launch {
            try {
                // If a new photo was selected, upload it first
                var finalPhotoUrl = user.profilePhotoUrl
                if (photoUri != null) {
                    val uploadResource = userRepository.uploadProfilePhoto(user.uid, photoUri).first { it !is Resource.Loading }
                    if (uploadResource is Resource.Success) {
                        finalPhotoUrl = uploadResource.data
                    } else if (uploadResource is Resource.Error) {
                        _uiState.value = EditProfileUiState.Error("Photo upload failed: ${uploadResource.error.message}")
                        return@launch
                    }
                }

                // Create updated user object
                val updatedUser = user.copy(
                    displayName = displayName.trim(),
                    phoneNumber = phoneNumber.trim(),
                    city = city.trim(),
                    weightKg = weight,
                    profilePhotoUrl = finalPhotoUrl
                )

                // Save to Firestore
                val saveResource = userRepository.saveUserProfile(updatedUser).first { it !is Resource.Loading }
                
                if (saveResource is Resource.Success) {
                    _uiState.value = EditProfileUiState.Success("Profile updated successfully!")
                } else if (saveResource is Resource.Error) {
                    _uiState.value = EditProfileUiState.Error(saveResource.error.message)
                }

            } catch (e: Exception) {
                _uiState.value = EditProfileUiState.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    fun resetState() {
        _uiState.value = EditProfileUiState.Idle
    }
}
