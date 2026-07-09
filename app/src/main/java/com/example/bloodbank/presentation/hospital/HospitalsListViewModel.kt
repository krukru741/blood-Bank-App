package com.example.bloodbank.presentation.hospital

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.model.HospitalMarker
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.repository.HospitalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HospitalsListUiState {
    object Loading : HospitalsListUiState()
    data class Success(val hospitals: List<HospitalMarker>) : HospitalsListUiState()
    data class Error(val message: String) : HospitalsListUiState()
}

@HiltViewModel
class HospitalsListViewModel @Inject constructor(
    private val repository: HospitalRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HospitalsListUiState> = combine(
        repository.getHospitals(),
        _searchQuery
    ) { resource, query ->
        when (resource) {
            is Resource.Loading -> HospitalsListUiState.Loading
            is Resource.Error -> HospitalsListUiState.Error(resource.error.message ?: "Failed to load hospitals")
            is Resource.Success -> {
                val allHospitals = resource.data
                val filtered = if (query.isBlank()) {
                    allHospitals
                } else {
                    allHospitals.filter {
                        it.name.contains(query, ignoreCase = true) ||
                        it.city?.contains(query, ignoreCase = true) == true ||
                        it.address.contains(query, ignoreCase = true)
                    }
                }
                HospitalsListUiState.Success(filtered.sortedBy { it.name })
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HospitalsListUiState.Loading
    )

    fun search(query: String) {
        _searchQuery.value = query
    }
}
