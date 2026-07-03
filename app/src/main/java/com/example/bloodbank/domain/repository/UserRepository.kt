package com.example.bloodbank.domain.repository

import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * UserRepository — CRUD operations for user profiles stored in Firestore.
 */
interface UserRepository {

    /** Fetches a user profile by their Firebase UID. */
    fun getUserById(uid: String): Flow<Resource<User>>

    /** Saves or updates the user's Firestore profile document. */
    fun saveUserProfile(user: User): Flow<Resource<Unit>>

    /** Updates only the profile photo URL (after uploading to Firebase Storage). */
    fun updateProfilePhotoUrl(uid: String, photoUrl: String): Flow<Resource<Unit>>
    
    /** Uploads a local photo (via Uri) to Firebase Storage and returns the download URL. */
    fun uploadProfilePhoto(uid: String, uri: android.net.Uri): Flow<Resource<String>>

    /** Returns a real-time stream of the current user's profile. */
    fun observeCurrentUser(uid: String): Flow<Resource<User>>

    /** Deletes the user's profile document (and optionally their auth account). */
    fun deleteUser(uid: String): Flow<Resource<Unit>>
    /**
     * Updates the user's FCM token in Firestore for push notifications.
     */
    fun updateFcmToken(userId: String, token: String): Flow<Resource<Unit>>
}
