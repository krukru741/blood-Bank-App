package com.example.bloodbank.data.repository

import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.BloodType
import com.example.bloodbank.domain.model.RequestStatus
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.UrgencyLevel
import com.example.bloodbank.domain.repository.BloodRequestRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BloodRequestRepositoryImpl
 *
 * Full Firestore-backed implementation.
 * Key features:
 * - [observeAllActiveRequests] — real-time listener via callbackFlow
 * - [observeMyRequests] — user-specific real-time listener
 * - [createRequest] — atomic Firestore write with generated ID
 * - [updateStatus] — partial update (only the status field)
 */
@Singleton
class BloodRequestRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BloodRequestRepository {

    companion object {
        private const val REQUESTS_COLLECTION = "blood_requests"
    }

    /**
     * Real-time stream of ALL active blood requests, ordered newest first.
     * Uses [callbackFlow] to bridge the Firestore snapshot listener to Flow.
     */
    override fun observeAllActiveRequests(): Flow<Resource<List<BloodRequest>>> = callbackFlow {
        trySend(Resource.Loading)

        val registration = firestore
            .collection(REQUESTS_COLLECTION)
            .whereEqualTo("status", RequestStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(error)))
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents
                    ?.mapNotNull { com.example.bloodbank.data.remote.mapper.BloodRequestMapper.mapToDomain(it) }
                    ?.sortedWith(
                        compareBy<BloodRequest> { it.urgency.ordinal } // Enum ordinal: CRITICAL(0), URGENT(1), NORMAL(2)
                            .thenByDescending { it.createdAt }
                    )
                    ?: emptyList()
                trySend(Resource.Success(requests))
            }

        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of requests belonging to a specific user.
     * Used in MyRequestsFragment for both donors and recipients.
     */
    override fun observeMyRequests(uid: String): Flow<Resource<List<BloodRequest>>> = callbackFlow {
        trySend(Resource.Loading)

        val registration = firestore
            .collection(REQUESTS_COLLECTION)
            .whereEqualTo("requesterId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(error)))
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents
                    ?.mapNotNull { com.example.bloodbank.data.remote.mapper.BloodRequestMapper.mapToDomain(it) }
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(Resource.Success(requests))
            }

        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of requests accepted by a specific donor.
     */
    override fun observeMyDonations(donorId: String): Flow<Resource<List<BloodRequest>>> = callbackFlow {
        trySend(Resource.Loading)

        val registration = firestore
            .collection(REQUESTS_COLLECTION)
            .whereEqualTo("acceptedByDonorId", donorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(error)))
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents
                    ?.mapNotNull { com.example.bloodbank.data.remote.mapper.BloodRequestMapper.mapToDomain(it) }
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(Resource.Success(requests))
            }

        awaitClose { registration.remove() }
    }

    /** Creates a new blood request document in Firestore. */
    override fun createRequest(request: BloodRequest): Flow<Resource<BloodRequest>> = flow {
        emit(Resource.Loading)
        try {
            // Generate a new document ID first so we can embed it in the document
            val docRef = firestore.collection(REQUESTS_COLLECTION).document()
            val requestWithId = request.copy(requestId = docRef.id)
            docRef.set(com.example.bloodbank.data.remote.mapper.BloodRequestMapper.mapToDto(requestWithId)).await()
            emit(Resource.Success(requestWithId))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override fun updateRequest(request: BloodRequest): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            firestore.collection(REQUESTS_COLLECTION)
                .document(request.requestId)
                .set(com.example.bloodbank.data.remote.mapper.BloodRequestMapper.mapToDto(request))
                .await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    /** Partially updates only the status field of an existing request. */
    override fun updateStatus(
        requestId: String,
        newStatus: RequestStatus
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            firestore.collection(REQUESTS_COLLECTION)
                .document(requestId)
                .update("status", newStatus.name)
                .await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override fun getRequestById(requestId: String): Flow<Resource<BloodRequest>> = flow {
        emit(Resource.Loading)
        try {
            val doc = firestore.collection(REQUESTS_COLLECTION).document(requestId).get().await()
            val request = com.example.bloodbank.data.remote.mapper.BloodRequestMapper.mapToDomain(doc) ?: throw Exception("Request not found")
            emit(Resource.Success(request))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    // ── Mapping helpers ────────────────────────────────────────────────────────

    // Replaced manual mapping with BloodRequestMapper.
    // The extension functions toBloodRequest() and toFirestoreMap() have been removed.
}
