package com.example.bloodbank.data.repository

import com.example.bloodbank.domain.model.BloodType
import com.example.bloodbank.domain.model.Gender
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.User
import com.example.bloodbank.domain.model.UserRole
import com.example.bloodbank.domain.repository.UserRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserRepositoryImpl — Firestore-backed user profile operations.
 *
 * Key improvements from Step 1 stub:
 * - Real Firestore → User domain model mapping via [DocumentSnapshot.toUser()]
 * - Real-time [observeCurrentUser] using callbackFlow + addSnapshotListener
 * - Proper error handling with try/catch
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
    }

    override fun getUserById(uid: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            val user = com.example.bloodbank.data.remote.mapper.UserMapper.mapToDomain(snapshot) ?: throw Exception("User not found")
            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override fun saveUserProfile(user: User): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(com.example.bloodbank.data.remote.mapper.UserMapper.mapToDto(user))
                .await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override fun updateProfilePhotoUrl(uid: String, photoUrl: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update("profilePhotoUrl", photoUrl)
                .await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override fun uploadProfilePhoto(uid: String, uri: android.net.Uri): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child("profile_photos/$uid/$fileName")
            
            // Upload the file
            storageRef.putFile(uri).await()
            
            // Get the download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            // Update Firestore with the new URL
            firestore.collection(USERS_COLLECTION).document(uid)
                .update("profilePhotoUrl", downloadUrl)
                .await()
                
            emit(Resource.Success(downloadUrl))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    /**
     * Real-time user profile stream using Firestore snapshot listener.
     * Uses [callbackFlow] to bridge the callback-based Firestore API to Kotlin Flow.
     */
    override fun observeCurrentUser(uid: String): Flow<Resource<User>> = callbackFlow {
        trySend(Resource.Loading)

        val listenerRegistration = firestore
            .collection(USERS_COLLECTION)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(error)))
                    return@addSnapshotListener
                }
                val user = snapshot?.let { com.example.bloodbank.data.remote.mapper.UserMapper.mapToDomain(it) }
                if (user != null) {
                    trySend(Resource.Success(user))
                } else {
                    trySend(Resource.Error(com.example.bloodbank.domain.error.AppError.NotFound("User document not found")))
                }
            }

        // Clean up listener when the Flow is cancelled (Fragment destroyed, etc.)
        awaitClose { listenerRegistration.remove() }
    }

    override fun deleteUser(uid: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            firestore.collection(USERS_COLLECTION).document(uid).delete().await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override fun updateFcmToken(userId: String, token: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            firestore.collection(USERS_COLLECTION).document(userId).update("fcmToken", token).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    // ── Mapping helpers ────────────────────────────────────────────────────────

    // Replaced manual mapping with UserMapper.
    // The extension functions toUser() and toFirestoreMap() have been removed.
}
