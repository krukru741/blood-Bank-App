package com.example.bloodbank.domain.repository

import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.RequestStatus
import com.example.bloodbank.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * BloodRequestRepository — manages blood donation requests in Firestore.
 *
 * All methods return [Flow<Resource<T>>] for consistent loading/success/error handling.
 */
interface BloodRequestRepository {

    /** Real-time stream of ALL active/pending requests ordered by urgency then time. */
    fun observeAllActiveRequests(): Flow<Resource<List<BloodRequest>>>

    /** Real-time stream of requests belonging to [uid] (their own requests). */
    fun observeMyRequests(uid: String): Flow<Resource<List<BloodRequest>>>

    /** Real-time stream of requests that a donor has accepted. */
    fun observeMyDonations(donorId: String): Flow<Resource<List<BloodRequest>>>

    /** Creates a new blood request. Returns the saved [BloodRequest] (with its new ID). */
    fun createRequest(request: BloodRequest): Flow<Resource<BloodRequest>>

    /** Updates an existing blood request. */
    fun updateRequest(request: BloodRequest): Flow<Resource<Unit>>

    /** Partially updates only the status field of an existing request. */
    fun updateStatus(requestId: String, newStatus: RequestStatus): Flow<Resource<Unit>>

    /** Fetches a single request by its Firestore document ID. */
    fun getRequestById(requestId: String): Flow<Resource<BloodRequest>>
}
