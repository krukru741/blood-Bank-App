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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RequestDetailUiState {
    object Loading : RequestDetailUiState()
    data class Success(
        val request: BloodRequest,
        val currentUser: com.example.bloodbank.domain.model.User? = null
    ) : RequestDetailUiState()
    data class Error(val message: String) : RequestDetailUiState()
}

@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    private val bloodRequestRepository: BloodRequestRepository,
    private val authRepository: AuthRepository,
    private val userRepository: com.example.bloodbank.domain.repository.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RequestDetailUiState>(RequestDetailUiState.Loading)
    val uiState: StateFlow<RequestDetailUiState> = _uiState.asStateFlow()

    val currentUserId = authRepository.currentUser?.uid

    fun loadRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.value = RequestDetailUiState.Loading
            try {
                val uid = currentUserId
                val userFlow = if (uid != null) {
                    userRepository.getUserById(uid)
                } else {
                    flowOf(Resource.Success(null))
                }

                bloodRequestRepository.getRequestById(requestId)
                    .combine(userFlow) { reqRes, userRes ->
                        if (reqRes is Resource.Loading || userRes is Resource.Loading) {
                            RequestDetailUiState.Loading
                        } else if (reqRes is Resource.Error) {
                            RequestDetailUiState.Error(reqRes.error.message)
                        } else if (reqRes is Resource.Success) {
                            val request = reqRes.data
                            val user = if (userRes is Resource.Success) userRes.data else null
                            RequestDetailUiState.Success(request, user)
                        } else {
                            RequestDetailUiState.Error("Unknown state")
                        }
                    }.collectLatest { state ->
                        _uiState.value = state
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
                        val currentState = _uiState.value
                        val user = if (currentState is RequestDetailUiState.Success) currentState.currentUser else null
                        _uiState.value = RequestDetailUiState.Success(updatedRequest, user)
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
