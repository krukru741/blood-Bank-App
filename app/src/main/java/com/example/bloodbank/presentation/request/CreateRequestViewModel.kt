package com.example.bloodbank.presentation.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.repository.PsgcRepository
import com.example.bloodbank.data.remote.api.dto.PsgcLocationDto
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.BloodType
import com.example.bloodbank.domain.model.RequestStatus
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.UrgencyLevel
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

enum class FormField {
    BLOOD_TYPE, UNITS, HOSPITAL, PROVINCE, CITY, CONTACT
}

data class FormErrors(
    val bloodType: String? = null,
    val units: String? = null,
    val hospital: String? = null,
    val province: String? = null,
    val city: String? = null,
    val contact: String? = null
)

data class CreateRequestUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val provinces: List<PsgcLocationDto> = emptyList(),
    val cities: List<PsgcLocationDto> = emptyList(),
    val barangays: List<PsgcLocationDto> = emptyList(),
    val formErrors: FormErrors = FormErrors(),
    val createdRequest: BloodRequest? = null,
    val psgcError: String? = null
)

@HiltViewModel
class CreateRequestViewModel @Inject constructor(
    private val bloodRequestRepository: BloodRequestRepository,
    private val authRepository: AuthRepository,
    private val psgcRepository: PsgcRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateRequestUiState())
    val uiState: StateFlow<CreateRequestUiState> = _uiState.asStateFlow()

    init {
        fetchProvinces()
    }

    fun fetchProvinces() {
        viewModelScope.launch {
            try {
                val provinces = psgcRepository.getProvinces().sortedBy { it.name }
                _uiState.update { it.copy(provinces = provinces, psgcError = null) }
            } catch (e: Exception) {
                if (_uiState.value.provinces.isEmpty()) {
                    _uiState.update { it.copy(psgcError = "Mahina ang internet. Subukang i-refresh o gamitin ang map.") }
                }
            }
        }
    }

    fun fetchCities(provinceCode: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(cities = emptyList(), barangays = emptyList()) }
                val cities = psgcRepository.getCitiesByProvince(provinceCode).sortedBy { it.name }
                _uiState.update { it.copy(cities = cities) }
            } catch (e: Exception) {}
        }
    }

    fun fetchBarangays(cityCode: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(barangays = emptyList()) }
                val barangays = psgcRepository.getBarangaysByCity(cityCode).sortedBy { it.name }
                _uiState.update { it.copy(barangays = barangays) }
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

    fun clearError(field: FormField) {
        _uiState.update { state ->
            val newErrors = when (field) {
                FormField.BLOOD_TYPE -> state.formErrors.copy(bloodType = null)
                FormField.UNITS      -> state.formErrors.copy(units = null)
                FormField.HOSPITAL   -> state.formErrors.copy(hospital = null)
                FormField.PROVINCE   -> state.formErrors.copy(province = null)
                FormField.CITY       -> state.formErrors.copy(city = null)
                FormField.CONTACT    -> state.formErrors.copy(contact = null)
            }
            state.copy(formErrors = newErrors)
        }
    }

    fun validateStep1(bloodType: String, units: String): Boolean {
        var isValid = true
        var bloodTypeError: String? = null
        var unitsError: String? = null

        if (bloodType.isBlank()) {
            bloodTypeError = "Blood type is required"
            isValid = false
        }
        if (units.isBlank() || (units.toIntOrNull() ?: 0) <= 0) {
            unitsError = "Enter a valid number (> 0)"
            isValid = false
        }

        _uiState.update { 
            it.copy(formErrors = it.formErrors.copy(bloodType = bloodTypeError, units = unitsError)) 
        }
        return isValid
    }

    fun validateStep2(hospital: String, province: String, city: String): Boolean {
        var isValid = true
        var hospitalError: String? = null
        var provinceError: String? = null
        var cityError: String? = null

        if (hospital.isBlank()) {
            hospitalError = "Hospital name is required"
            isValid = false
        }
        if (province.isBlank()) {
            provinceError = "Province is required"
            isValid = false
        }
        if (city.isBlank()) {
            cityError = "City is required"
            isValid = false
        }

        _uiState.update {
            it.copy(formErrors = it.formErrors.copy(hospital = hospitalError, province = provinceError, city = cityError))
        }
        return isValid
    }

    fun validateStep3(contact: String): Boolean {
        var isValid = true
        var contactError: String? = null

        if (contact.isBlank()) {
            contactError = "Contact number is required"
            isValid = false
        } else if (contact.length < 10) {
            contactError = "Enter a valid contact number"
            isValid = false
        }

        _uiState.update {
            it.copy(formErrors = it.formErrors.copy(contact = contactError))
        }
        return isValid
    }

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
        if (!validateStep1(bloodTypeLabel, units) || !validateStep2(hospital, province, city) || !validateStep3(contact)) {
            return
        }

        val currentUser = authRepository.currentUser
        if (currentUser == null) {
            _uiState.update { it.copy(errorMessage = "Not authenticated") }
            return
        }

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
            unitsNeeded   = units.toIntOrNull() ?: 1,
            hospital      = hospital,
            location      = fullLocation,
            latitude      = latitude,
            longitude     = longitude,
            urgency       = urgency,
            status        = RequestStatus.PENDING,
            description   = notes,
            contactNumber = contact,
            createdAt     = now,
            expiresAt     = now + (7L * 24 * 60 * 60 * 1000)
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            bloodRequestRepository.createRequest(request).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        if (!currentUser.hasCreatedRequest) {
                            val updatedUser = currentUser.copy(hasCreatedRequest = true)
                            userRepository.saveUserProfile(updatedUser).collect { saveResource ->
                                if (saveResource !is Resource.Loading) {
                                    _uiState.update { it.copy(isLoading = false, isSuccess = true, createdRequest = resource.data) }
                                }
                            }
                        } else {
                            _uiState.update { it.copy(isLoading = false, isSuccess = true, createdRequest = resource.data) }
                        }
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = resource.error.message) }
                }
            }
        }
    }

    fun resetState() {
        _uiState.update { it.copy(isSuccess = false, errorMessage = null, isLoading = false) }
    }
}
