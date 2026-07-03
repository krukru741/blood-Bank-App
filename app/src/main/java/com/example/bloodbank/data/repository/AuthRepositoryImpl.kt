package com.example.bloodbank.data.repository

import com.example.bloodbank.domain.model.BloodType
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.User
import com.example.bloodbank.domain.model.UserRole
import com.example.bloodbank.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthRepositoryImpl
 *
 * Concrete implementation of [AuthRepository] backed by Firebase Auth + Firestore.
 * @Inject constructor makes Hilt aware of this class — it will inject
 * [FirebaseAuth] and [FirebaseFirestore] from [FirebaseModule].
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
    }

    override val currentUser: User?
        get() = firebaseAuth.currentUser?.let { fbUser ->
            User(
                uid         = fbUser.uid,
                email       = fbUser.email.orEmpty(),
                displayName = fbUser.displayName.orEmpty(),
                isVerified  = fbUser.isEmailVerified
            )
        }

    override val isLoggedIn: Boolean
        get() {
            val user = firebaseAuth.currentUser
            // User is only considered logged in if they exist AND their email is verified
            return user != null && user.isEmailVerified
        }

    override fun register(
        email: String,
        password: String,
        displayName: String,
        phoneNumber: String
    ): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            // 1. Create Firebase Auth account
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val fbUser = authResult.user
                ?: throw IllegalStateException("Firebase user is null after registration")

            // 2. Update display name in Firebase Auth profile
            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            fbUser.updateProfile(profileUpdate).await()

            // 3. Build the minimal User model (just auth info).
            //    The FULL profile (role, gender, weight, hospital, etc.) is saved
            //    by RegisterViewModel via UserRepository.saveUserProfile() next.
            val user = User(
                uid         = fbUser.uid,
                email       = email,
                displayName = displayName,
                phoneNumber = phoneNumber,
                isVerified  = false,
                createdAt   = System.currentTimeMillis()
            )

            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override fun login(email: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val authResult = firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()

            val fbUser = authResult.user
                ?: throw IllegalStateException("Firebase user is null after login")

            // BLOCK LOGIN IF NOT VERIFIED
            if (!fbUser.isEmailVerified) {
                firebaseAuth.signOut() // Immediately log them out so session is not persisted
                throw Exception("Please verify your email address before logging in.")
            }

            // Fetch full profile from Firestore
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(fbUser.uid)
                .get()
                .await()

            // Map Firestore document to User — fall back to basic auth info if no doc yet
            val user = if (snapshot.exists()) {
                com.example.bloodbank.data.remote.mapper.UserMapper.mapToDomain(snapshot) ?: User(
                    uid         = fbUser.uid,
                    email       = fbUser.email.orEmpty(),
                    displayName = fbUser.displayName.orEmpty(),
                    isVerified  = fbUser.isEmailVerified
                )
            } else {
                // First login after registration (profile doc may not exist yet)
                User(
                    uid         = fbUser.uid,
                    email       = fbUser.email.orEmpty(),
                    displayName = fbUser.displayName.orEmpty(),
                    isVerified  = fbUser.isEmailVerified
                )
            }

            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override fun signInWithGoogle(idToken: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val fbUser = authResult.user
                ?: throw IllegalStateException("Firebase user is null after Google login")

            // Fetch full profile from Firestore
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(fbUser.uid)
                .get()
                .await()

            val user = if (snapshot.exists()) {
                com.example.bloodbank.data.remote.mapper.UserMapper.mapToDomain(snapshot) ?: User(
                    uid         = fbUser.uid,
                    email       = fbUser.email.orEmpty(),
                    displayName = fbUser.displayName.orEmpty(),
                    isVerified  = true // Google emails are implicitly verified
                )
            } else {
                // First time Google Login -> Build a default user object.
                // Normally they should go through onboarding to pick a role.
                val newUser = User(
                    uid         = fbUser.uid,
                    email       = fbUser.email.orEmpty(),
                    displayName = fbUser.displayName.orEmpty(),
                    isVerified  = true
                )
                // Save basic info to Firestore
                val userMap = hashMapOf(
                    "uid" to newUser.uid,
                    "email" to newUser.email,
                    "displayName" to newUser.displayName,
                    "isVerified" to newUser.isVerified,
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection(USERS_COLLECTION).document(newUser.uid).set(userMap).await()
                newUser
            }

            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override fun signInWithFacebook(accessToken: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val credential = com.google.firebase.auth.FacebookAuthProvider.getCredential(accessToken)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val fbUser = authResult.user
                ?: throw IllegalStateException("Firebase user is null after Facebook login")

            // Fetch full profile from Firestore
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(fbUser.uid)
                .get()
                .await()

            val user = if (snapshot.exists()) {
                com.example.bloodbank.data.remote.mapper.UserMapper.mapToDomain(snapshot) ?: User(
                    uid         = fbUser.uid,
                    email       = fbUser.email.orEmpty(),
                    displayName = fbUser.displayName.orEmpty(),
                    isVerified  = true // FB emails are implicitly verified in Firebase if they have one
                )
            } else {
                val newUser = User(
                    uid         = fbUser.uid,
                    email       = fbUser.email.orEmpty(),
                    displayName = fbUser.displayName.orEmpty(),
                    isVerified  = true
                )
                val userMap = hashMapOf(
                    "uid" to newUser.uid,
                    "email" to newUser.email,
                    "displayName" to newUser.displayName,
                    "isVerified" to newUser.isVerified,
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection(USERS_COLLECTION).document(newUser.uid).set(userMap).await()
                newUser
            }

            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override fun sendPasswordResetEmail(email: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override fun sendEmailVerification(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            firebaseAuth.currentUser?.sendEmailVerification()?.await()
                ?: throw IllegalStateException("No authenticated user")
            
            // Sign out immediately so they have to verify and log in properly
            firebaseAuth.signOut()
            
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override suspend fun reloadUser() {
        firebaseAuth.currentUser?.reload()?.await()
    }

    // The manual User.toFirestoreMap() has been removed and is now handled by UserMapper.
}
