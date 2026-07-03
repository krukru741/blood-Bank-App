package com.example.bloodbank.presentation.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.BloodType
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.UrgencyLevel
import com.example.bloodbank.domain.repository.BloodRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditRequestViewModel @Inject constructor(
    private val bloodRequestRepository: BloodRequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditRequestUiState>(EditRequestUiState.Idle)
    val uiState: StateFlow<EditRequestUiState> = _uiState.asStateFlow()

    private var currentRequest: BloodRequest? = null

    val bloodTypeOptions: List<String> = BloodType.entries.map { it.label }

    fun loadRequest(requestId: String) {
        viewModelScope.launch {
            bloodRequestRepository.getRequestById(requestId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = EditRequestUiState.Loading
                    is Resource.Success -> {
                        currentRequest = resource.data
                        _uiState.value = EditRequestUiState.Loaded(resource.data)
                    }
                    is Resource.Error -> _uiState.value = EditRequestUiState.Error(resource.error.message)
                }
            }
        }
    }

    fun updateRequest(
        bloodTypeLabel: String,
        units: String,
        hospital: String,
        location: String,
        contact: String,
        notes: String,
        urgency: UrgencyLevel
    ) {
        val requestToUpdate = currentRequest ?: run {
            _uiState.value = EditRequestUiState.Error("No active request to update")
            return
        }

        // Validation
        if (hospital.isBlank()) { _uiState.value = EditRequestUiState.Error("Hospital name is required"); return }
        if (location.isBlank()) { _uiState.value = EditRequestUiState.Error("City/location is required"); return }
        if (contact.isBlank())  { _uiState.value = EditRequestUiState.Error("Contact number is required"); return }
        val unitsInt = units.toIntOrNull()?.takeIf { it > 0 }
            ?: run { _uiState.value = EditRequestUiState.Error("Enter a valid number of units (min 1)"); return }

        val updatedRequest = requestToUpdate.copy(
            bloodType = BloodType.fromLabel(bloodTypeLabel),
            unitsNeeded = unitsInt,
            hospital = hospital,
            location = location,
            contactNumber = contact,
            description = notes,
            urgency = urgency
        )

        viewModelScope.launch {
            bloodRequestRepository.updateRequest(updatedRequest).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = EditRequestUiState.Loading
                    is Resource.Success -> _uiState.value = EditRequestUiState.Updated
                    is Resource.Error -> _uiState.value = EditRequestUiState.Error(resource.error.message)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = EditRequestUiState.Idle
    }
}
