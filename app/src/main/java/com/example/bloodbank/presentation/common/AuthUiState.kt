package com.example.bloodbank.presentation.common

/**
 * AuthUiState — shared sealed class for auth screen states.
 * Each ViewModel emits this to the Fragment via StateFlow.
 *
 * Using a generic T allows each screen to specify its own
 * Success payload (User, Unit, String, etc.)
 */
sealed class AuthUiState<out T> {

    /** Initial state — nothing happening yet. */
    data object Idle : AuthUiState<Nothing>()

    /** Operation is in progress — show loading indicator. */
    data object Loading : AuthUiState<Nothing>()

    /** Operation succeeded. [data] is the result payload. */
    data class Success<T>(val data: T) : AuthUiState<T>()

    /** Operation failed. [message] is a human-readable error. */
    data class Error(val message: String) : AuthUiState<Nothing>()
}
