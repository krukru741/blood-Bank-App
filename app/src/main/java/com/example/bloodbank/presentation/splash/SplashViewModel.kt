package com.example.bloodbank.presentation.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SplashViewModel
 *
 * @HiltViewModel — allows Hilt to inject dependencies into this ViewModel.
 * @Inject constructor — Hilt provides [AuthRepository] automatically.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    enum class Destination { MAIN, AUTH }

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _navigationDestination = MutableLiveData<Destination>()
    val navigationDestination: LiveData<Destination> = _navigationDestination

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            // Delay to allow the user to read the custom splash screen text
            delay(1500)

            _navigationDestination.value = if (authRepository.isLoggedIn) {
                Destination.MAIN
            } else {
                Destination.AUTH
            }

            _isLoading.value = false
        }
    }
}
