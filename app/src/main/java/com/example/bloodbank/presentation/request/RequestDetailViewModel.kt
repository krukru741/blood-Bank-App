package com.example.bloodbank.presentation.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.domain.repository.BloodRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RequestDetailUiState {
    object Loading : RequestDetailUiState()
    data class Success(val request: BloodRequest) : RequestDetailUiState()
    data class Error(val message: String) : RequestDetailUiState()
}

@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    private val bloodRequestRepository: BloodRequestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RequestDetailUiState>(RequestDetailUiState.Loading)
    val uiState: StateFlow<RequestDetailUiState> = _uiState.asStateFlow()

    val currentUserId = authRepository.currentUser?.uid

    fun loadRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.value = RequestDetailUiState.Loading
            try {
                bloodRequestRepository.getRequestById(requestId).collectLatest { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val request = resource.data
                            if (request != null) {
                                _uiState.value = RequestDetailUiState.Success(request)
                            } else {
                                _uiState.value = RequestDetailUiState.Error("Request not found")
                            }
                        }
                        is Resource.Error -> {
                            _uiState.value = RequestDetailUiState.Error(resource.error.message)
                        }
                        is Resource.Loading -> {} // handled
                    }
                }
            } catch (e: Exception) {
                _uiState.value = RequestDetailUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun acceptRequest(request: BloodRequest) {
        val currentUser = authRepository.currentUser ?: return
        
        val updatedRequest = request.copy(
            status = com.example.bloodbank.domain.model.RequestStatus.MATCHED,
            acceptedByDonorId = currentUser.uid,
            acceptedByDonorName = currentUser.displayName,
            acceptedByDonorPhone = currentUser.phoneNumber
        )

        viewModelScope.launch {
            _uiState.value = RequestDetailUiState.Loading
            bloodRequestRepository.updateRequest(updatedRequest).collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = RequestDetailUiState.Success(updatedRequest)
                    }
                    is Resource.Error -> {
                        _uiState.value = RequestDetailUiState.Error(resource.error.message)
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }
}
