package com.example.bloodbank.presentation.hospital

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.model.HospitalMarker
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.repository.HospitalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddHospitalUiState {
    object Idle : AddHospitalUiState()
    object Loading : AddHospitalUiState()
    object Success : AddHospitalUiState()
    data class Error(val message: String) : AddHospitalUiState()
}

@HiltViewModel
class AddHospitalViewModel @Inject constructor(
    private val repository: HospitalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddHospitalUiState>(AddHospitalUiState.Idle)
    val uiState: StateFlow<AddHospitalUiState> = _uiState.asStateFlow()

    fun addHospital(
        name: String,
        address: String,
        city: String,
        contact: String,
        emergencyContact: String,
        type: String,
        latitudeStr: String,
        longitudeStr: String
    ) {
        if (name.isBlank() || latitudeStr.isBlank() || longitudeStr.isBlank()) {
            _uiState.value = AddHospitalUiState.Error("Name, Latitude, and Longitude are required.")
            return
        }

        val latitude = latitudeStr.toDoubleOrNull()
        val longitude = longitudeStr.toDoubleOrNull()

        if (latitude == null || longitude == null) {
            _uiState.value = AddHospitalUiState.Error("Invalid coordinates. Please enter numbers.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AddHospitalUiState.Loading

            val newHospital = HospitalMarker(
                id = "", // Let repository generate this
                name = name.trim(),
                address = address.trim(),
                city = city.takeIf { it.isNotBlank() }?.trim(),
                contactNumber = contact.takeIf { it.isNotBlank() }?.trim(),
                emergencyContact = emergencyContact.takeIf { it.isNotBlank() }?.trim(),
                type = type.takeIf { it.isNotBlank() }?.trim(),
                latitude = latitude,
                longitude = longitude
            )

            when (val result = repository.addHospital(newHospital)) {
                is Resource.Success -> _uiState.value = AddHospitalUiState.Success
                is Resource.Error -> _uiState.value = AddHospitalUiState.Error(result.error.message ?: "Failed to add hospital")
                is Resource.Loading -> Unit // Should not happen for suspend funs usually, but just in case
            }
        }
    }

    fun resetState() {
        _uiState.value = AddHospitalUiState.Idle
    }
}
