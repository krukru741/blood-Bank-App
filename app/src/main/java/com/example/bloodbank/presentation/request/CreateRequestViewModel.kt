package com.example.bloodbank.presentation.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.data.remote.api.PsgcApiService
import com.example.bloodbank.data.remote.api.dto.PsgcLocationDto
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.BloodType
import com.example.bloodbank.domain.model.RequestStatus
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.UrgencyLevel
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.domain.repository.BloodRequestRepository
import com.example.bloodbank.presentation.common.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRequestViewModel @Inject constructor(
    private val bloodRequestRepository: BloodRequestRepository,
    private val authRepository: AuthRepository,
    private val psgcApiService: PsgcApiService,
    private val userRepository: com.example.bloodbank.domain.repository.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState<BloodRequest>>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState<BloodRequest>> = _uiState.asStateFlow()

    private val _provinces = MutableStateFlow<List<PsgcLocationDto>>(emptyList())
    val provinces: StateFlow<List<PsgcLocationDto>> = _provinces.asStateFlow()

    private val _cities = MutableStateFlow<List<PsgcLocationDto>>(emptyList())
    val cities: StateFlow<List<PsgcLocationDto>> = _cities.asStateFlow()

    private val _barangays = MutableStateFlow<List<PsgcLocationDto>>(emptyList())
    val barangays: StateFlow<List<PsgcLocationDto>> = _barangays.asStateFlow()

    init {
        fetchProvinces()
    }

    private fun fetchProvinces() {
        viewModelScope.launch {
            try {
                _provinces.value = psgcApiService.getProvinces().sortedBy { it.name }
            } catch (e: Exception) {}
        }
    }

    fun fetchCities(provinceCode: String) {
        viewModelScope.launch {
            try {
                _cities.value = emptyList()
                _barangays.value = emptyList()
                _cities.value = psgcApiService.getCitiesByProvince(provinceCode).sortedBy { it.name }
            } catch (e: Exception) {}
        }
    }

    fun fetchBarangays(cityCode: String) {
        viewModelScope.launch {
            try {
                _barangays.value = emptyList()
                _barangays.value = psgcApiService.getBarangaysByCity(cityCode).sortedBy { it.name }
            } catch (e: Exception) {}
        }
    }

    val prefill: PrefillData by lazy {
        val user = authRepository.currentUser
        PrefillData(
            bloodType    = user?.bloodType?.label ?: "O+",
            hospitalName = user?.hospitalName     ?: "",
            phone        = user?.phoneNumber      ?: ""
        )
    }

    val bloodTypeOptions: List<String> = BloodType.entries.map { it.label }

    data class PrefillData(
        val bloodType: String,
        val hospitalName: String,
        val phone: String
    )

    fun submitRequest(
        bloodTypeLabel: String,
        units: String,
        hospital: String,
        province: String,
        city: String,
        barangay: String,
        street: String,
        latitude: Double?,
        longitude: Double?,
        contact: String,
        notes: String,
        urgency: UrgencyLevel
    ) {
        if (hospital.isBlank()) { _uiState.value = AuthUiState.Error("Hospital name is required"); return }
        if (province.isBlank()) { _uiState.value = AuthUiState.Error("Province is required"); return }
        if (city.isBlank())     { _uiState.value = AuthUiState.Error("City is required"); return }
        if (contact.isBlank())  { _uiState.value = AuthUiState.Error("Contact number is required"); return }
        val unitsInt = units.toIntOrNull()?.takeIf { it > 0 }
            ?: run { _uiState.value = AuthUiState.Error("Enter a valid number of units (min 1)"); return }

        val currentUser = authRepository.currentUser
            ?: run { _uiState.value = AuthUiState.Error("Not authenticated"); return }

        // Compile full location string
        val fullLocation = listOfNotNull(
            street.takeIf { it.isNotBlank() },
            barangay.takeIf { it.isNotBlank() },
            city,
            province
        ).joinToString(", ")

        val now = System.currentTimeMillis()
        val request = BloodRequest(
            requesterId   = currentUser.uid,
            requesterName = currentUser.displayName,
            bloodType     = BloodType.fromLabel(bloodTypeLabel),
            unitsNeeded   = unitsInt,
            hospital      = hospital,
            location      = fullLocation, // Using full location instead of just city
            latitude      = latitude,
            longitude     = longitude,
            urgency       = urgency,
            status        = RequestStatus.PENDING,
            description   = notes,
            contactNumber = contact,
            createdAt     = now,
            expiresAt     = now + (7L * 24 * 60 * 60 * 1000) // 7 days
        )

        viewModelScope.launch {
            bloodRequestRepository.createRequest(request).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = AuthUiState.Loading
                    is Resource.Success -> {
                        if (!currentUser.hasCreatedRequest) {
                            val updatedUser = currentUser.copy(hasCreatedRequest = true)
                            userRepository.saveUserProfile(updatedUser).collect { saveResource ->
                                if (saveResource !is Resource.Loading) {
                                    _uiState.value = AuthUiState.Success(resource.data)
                                }
                            }
                        } else {
                            _uiState.value = AuthUiState.Success(resource.data)
                        }
                    }
                    is Resource.Error   -> _uiState.value = AuthUiState.Error(resource.error.message)
                }
            }
        }
    }

    fun resetState() { _uiState.value = AuthUiState.Idle }
}
