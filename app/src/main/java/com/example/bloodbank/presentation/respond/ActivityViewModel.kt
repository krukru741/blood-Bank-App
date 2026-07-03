package com.example.bloodbank.presentation.respond

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.UserRole
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.domain.repository.BloodRequestRepository
import com.example.bloodbank.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityUiState(
    val isLoading: Boolean = true,
    val isDonor: Boolean = false,
    val donations: List<BloodRequest> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val bloodRequestRepository: BloodRequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        observeUserRoleAndDonations()
    }

    private fun observeUserRoleAndDonations() {
        val uid = authRepository.currentUser?.uid ?: return
        
        viewModelScope.launch {
            userRepository.observeCurrentUser(uid).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val isDonor = resource.data.role == UserRole.DONOR
                        _uiState.update { it.copy(isDonor = isDonor) }
                        
                        if (isDonor) {
                            observeMyDonations(uid)
                        } else {
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = resource.error.message) }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun observeMyDonations(uid: String) {
        viewModelScope.launch {
            bloodRequestRepository.observeMyDonations(uid).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.update { 
                            it.copy(isLoading = false, donations = resource.data, error = null) 
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { 
                            it.copy(isLoading = false, error = resource.error.message) 
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }
}
