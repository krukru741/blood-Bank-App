package com.example.bloodbank.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.model.BloodType
import com.example.bloodbank.domain.model.Gender
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.UserRole
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.domain.repository.UserRepository
import com.example.bloodbank.presentation.common.AuthUiState
import com.example.bloodbank.presentation.common.validation.FormValidator
import com.example.bloodbank.data.remote.api.PsgcApiService
import com.example.bloodbank.data.remote.api.dto.PsgcLocationDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BecomeDonorViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val psgcApiService: PsgcApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState<Unit>>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState<Unit>> = _uiState.asStateFlow()

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

    val bloodTypeOptions: List<String> = BloodType.entries.map { it.label }
    val genderOptions: List<String>    = listOf(Gender.MALE.label, Gender.FEMALE.label)

    fun saveDonorProfile(
        bloodTypeLabel: String,
        genderLabel: String,
        dateOfBirth: Long?,
        weightText: String,
        lastDonationDate: Long?,
        province: String,
        city: String,
        barangay: String,
        street: String,
        latitude: Double?,
        longitude: Double?
    ) {
        val provinceError = FormValidator.validateProvince(province)
        val cityError     = FormValidator.validateCity(city)
        val barangayError = FormValidator.validateBarangay(barangay)
        val streetError   = FormValidator.validateStreet(street)
        val dobError      = FormValidator.validateDateOfBirth(dateOfBirth)
        val weightError   = FormValidator.validateWeight(weightText)
        val lastDonError  = FormValidator.validateLastDonationDate(lastDonationDate)

        val firstError = listOf(
            provinceError, cityError, barangayError, streetError,
            dobError, weightError, lastDonError
        ).firstOrNull { it != null }

        if (firstError != null) {
            _uiState.value = AuthUiState.Error(firstError)
            return
        }

        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            userRepository.getUserById(uid).collect { resource ->
                if (resource is Resource.Success) {
                    val updatedUser = resource.data.copy(
                        bloodType        = BloodType.fromLabel(bloodTypeLabel),
                        gender           = Gender.fromLabel(genderLabel),
                        dateOfBirth      = dateOfBirth,
                        weightKg         = weightText.toFloatOrNull(),
                        lastDonationDate = lastDonationDate,
                        role             = UserRole.DONOR,
                        province         = province,
                        city             = city,
                        barangay         = barangay,
                        street           = street,
                        latitude         = latitude,
                        longitude        = longitude
                    )
                    userRepository.saveUserProfile(updatedUser).collect { saveResource ->
                        when (saveResource) {
                            is Resource.Loading -> {}
                            is Resource.Success -> _uiState.value = AuthUiState.Success(Unit)
                            is Resource.Error   -> _uiState.value = AuthUiState.Error("Failed to update profile.")
                        }
                    }
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
