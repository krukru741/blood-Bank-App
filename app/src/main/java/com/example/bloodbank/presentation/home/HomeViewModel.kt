package com.example.bloodbank.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.BloodType
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.UrgencyLevel
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.domain.repository.BloodRequestRepository
import com.example.bloodbank.domain.repository.UserRepository
import com.example.bloodbank.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.bloodbank.domain.model.HospitalMarker
import com.example.bloodbank.domain.model.MockHospitalData
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Filter options for the home feed chips. */
enum class FeedFilter { ALL, CRITICAL, URGENT, MY_TYPE, HOSPITALS }

/** UI state for the home screen. */
data class HomeUiState(
    val isLoading: Boolean                  = true,
    val allRequests: List<BloodRequest>     = emptyList(),
    val filteredRequests: List<BloodRequest> = emptyList(),
    val activeFilter: FeedFilter            = FeedFilter.ALL,
    val criticalCount: Int                  = 0,
    val urgentCount: Int                    = 0,
    val totalCount: Int                     = 0,
    val myTypeCount: Int                    = 0,
    val currentUserBloodType: BloodType?    = null,
    val isRecipient: Boolean                = false,
    val error: String?                      = null,
    val hospitals: List<HospitalMarker>     = emptyList(),
    val userLocation: Pair<Double, Double>? = null
)

/**
 * HomeViewModel
 *
 * - Subscribes to [BloodRequestRepository.observeAllActiveRequests] on init (real-time)
 * - Applies [FeedFilter] to produce [filteredRequests]
 * - Computes summary counts for the banner (critical, total, my-type)
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bloodRequestRepository: BloodRequestRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeUser()
        observeRequests()
    }

    private fun observeUser() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.observeCurrentUser(uid).collect { resource ->
                if (resource is Resource.Success) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentUserBloodType = resource.data.bloodType,
                            isRecipient = resource.data.role == UserRole.RECIPIENT
                        ).applyFilter()
                    }
                }
            }
        }
    }

    private fun observeRequests() {
        viewModelScope.launch {
            bloodRequestRepository.observeAllActiveRequests().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { 
                        it.copy(isLoading = true) 
                    }

                    is Resource.Success -> {
                        _uiState.update { currentState ->
                            val all = resource.data
                            val myType = currentState.currentUserBloodType
                            currentState.copy(
                                isLoading      = false,
                                allRequests    = all,
                                criticalCount  = all.count { it.urgency == UrgencyLevel.CRITICAL },
                                urgentCount    = all.count { it.urgency == UrgencyLevel.URGENT },
                                totalCount     = all.size,
                                myTypeCount    = if (myType != null) all.count { it.bloodType == myType } else 0,
                                error          = null
                            ).applyFilter()
                        }
                    }

                    is Resource.Error -> _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error     = resource.error.message
                        )
                    }
                }
            }
        }
    }

    /** Called when user selects a filter chip. */
    fun setFilter(filter: FeedFilter) {
        _uiState.update { 
            it.copy(activeFilter = filter).applyFilter() 
        }
    }

    private fun HomeUiState.applyFilter(): HomeUiState {
        val filtered = when (activeFilter) {
            FeedFilter.ALL      -> allRequests
            FeedFilter.CRITICAL -> allRequests.filter { it.urgency == UrgencyLevel.CRITICAL }
            FeedFilter.URGENT   -> allRequests.filter { it.urgency == UrgencyLevel.URGENT }
            FeedFilter.MY_TYPE  -> allRequests.filter {
                currentUserBloodType != null && it.bloodType == currentUserBloodType
            }
            FeedFilter.HOSPITALS -> emptyList()
        }
        val activeHospitals = if (activeFilter == FeedFilter.HOSPITALS) MockHospitalData.hospitals else emptyList()
        return copy(filteredRequests = filtered, hospitals = activeHospitals)
    }

    /** Mock updating user location */
    fun updateUserLocation(lat: Double, lng: Double) {
        _uiState.update { it.copy(userLocation = Pair(lat, lng)) }
    }
}
