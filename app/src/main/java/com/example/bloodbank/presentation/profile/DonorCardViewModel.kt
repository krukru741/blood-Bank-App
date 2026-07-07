package com.example.bloodbank.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.User
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DonorCardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserById(uid).collect { resource ->
                if (resource is Resource.Success) {
                    _user.value = resource.data
                }
            }
        }
    }
}
