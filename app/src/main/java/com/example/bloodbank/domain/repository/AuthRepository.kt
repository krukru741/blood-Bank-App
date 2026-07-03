package com.example.bloodbank.domain.repository

import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * AuthRepository
 *
 * Contract (interface) for all authentication operations.
 * Defined in the **Domain** layer — the Data layer provides the implementation.
 * ViewModels depend on this interface, never on the concrete implementation.
 */
interface AuthRepository {

    /** Returns the currently signed-in [User], or null if not authenticated. */
    val currentUser: User?

    /** True if a user session is active. */
    val isLoggedIn: Boolean

    /**
     * Registers a new user with email + password.
     * Emits [Resource.Loading] → [Resource.Success]/[Resource.Error].
     */
    fun register(
        email: String,
        password: String,
        displayName: String,
        phoneNumber: String
    ): Flow<Resource<User>>

    /**
     * Signs in an existing user with email + password.
     * Emits [Resource.Loading] → [Resource.Success]/[Resource.Error].
     */
    fun login(email: String, password: String): Flow<Resource<User>>

    /**
     * Signs in using Google ID Token.
     */
    fun signInWithGoogle(idToken: String): Flow<Resource<User>>

    /**
     * Signs in using Facebook Access Token.
     */
    fun signInWithFacebook(accessToken: String): Flow<Resource<User>>

    /** Signs out the current user and clears any cached session. */
    suspend fun logout()

    /**
     * Sends a password reset email.
     * Emits [Resource.Success] (Unit) on success, [Resource.Error] on failure.
     */
    fun sendPasswordResetEmail(email: String): Flow<Resource<Unit>>

    /**
     * Sends an email verification to the current user's registered email.
     */
    fun sendEmailVerification(): Flow<Resource<Unit>>

    /** Reloads the current user's profile from Firebase (to check verification). */
    suspend fun reloadUser()
}
