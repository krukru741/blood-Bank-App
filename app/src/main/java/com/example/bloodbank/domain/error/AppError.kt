package com.example.bloodbank.domain.error

sealed class AppError {
    
    // Abstract property so every error can provide a user-friendly message
    abstract val message: String

    // Network & Connectivity
    data class Network(override val message: String = "No internet connection or network error.") : AppError()
    data class Timeout(override val message: String = "The request timed out.") : AppError()

    // Authentication
    data class InvalidCredentials(override val message: String = "Invalid email or password.") : AppError()
    data class EmailAlreadyInUse(override val message: String = "This email is already in use by another account.") : AppError()
    data class WeakPassword(override val message: String = "The password is too weak.") : AppError()
    data class UserNotFound(override val message: String = "User account not found.") : AppError()
    data class UnverifiedEmail(override val message: String = "Please verify your email address before logging in.") : AppError()
    
    // Firestore Data
    data class NotFound(override val message: String = "The requested data was not found.") : AppError()
    data class PermissionDenied(override val message: String = "You don't have permission to perform this action.") : AppError()

    // Generic Fallback
    data class Unknown(override val message: String = "An unknown error occurred.") : AppError()
}
